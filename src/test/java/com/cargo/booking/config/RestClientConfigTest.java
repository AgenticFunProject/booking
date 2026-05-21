package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    private final List<CapturedRequest> capturedRequests = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;
    private ExecutorService executorService;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RestClientConfig.class, InterceptorConfig.class);

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handleRequest);
        server.createContext("/slow", this::handleSlowRequest);
        executorService = Executors.newSingleThreadExecutor();
        server.setExecutor(executorService);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        executorService.shutdownNow();
        capturedRequests.clear();
        InterceptorConfig.interceptedUris.clear();
    }

    @Test
    void shouldCreateQualifiedRestClientsWithBaseUrlsJsonHeadersAndInterceptors() {
        contextRunner
                .withPropertyValues(
                        "app.integration.schedule-api.base-url=" + serverUrl("/schedule"),
                        "app.integration.equipment-api.base-url=" + serverUrl("/equipment"),
                        "app.integration.quote-api.base-url=" + serverUrl("/quote")
                )
                .run(context -> {
                    RestClient scheduleRestClient = context.getBean("scheduleRestClient", RestClient.class);
                    RestClient equipmentRestClient = context.getBean("equipmentRestClient", RestClient.class);
                    RestClient quoteRestClient = context.getBean("quoteRestClient", RestClient.class);

                    scheduleRestClient.get().uri("/availability").retrieve().toBodilessEntity();
                    equipmentRestClient.get().uri("/reservations").retrieve().toBodilessEntity();
                    quoteRestClient.get().uri("/quotes").retrieve().toBodilessEntity();

                    assertThat(capturedRequests)
                            .extracting(CapturedRequest::path)
                            .containsExactlyInAnyOrder(
                                    "/schedule/availability",
                                    "/equipment/reservations",
                                    "/quote/quotes"
                            );
                    assertThat(capturedRequests)
                            .allSatisfy(request -> {
                                assertThat(request.accept()).contains("application/json");
                                assertThat(request.contentType()).contains("application/json");
                            });
                    assertThat(InterceptorConfig.interceptedUris)
                            .extracting(URI::getPath)
                            .containsExactlyInAnyOrder(
                                    "/schedule/availability",
                                    "/equipment/reservations",
                                    "/quote/quotes"
                            );
                });
    }

    @Test
    void shouldApplyConfiguredReadTimeout() {
        contextRunner
                .withPropertyValues(
                        "app.integration.schedule-api.base-url=" + serverUrl("/slow"),
                        "app.integration.schedule-api.timeout-ms=50"
                )
                .run(context -> {
                    RestClient scheduleRestClient = context.getBean("scheduleRestClient", RestClient.class);

                    assertThatThrownBy(() -> scheduleRestClient.get().uri("/response").retrieve().toBodilessEntity())
                            .isInstanceOf(ResourceAccessException.class);
                });
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        capturedRequests.add(new CapturedRequest(
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Accept"),
                exchange.getRequestHeaders().getFirst("Content-Type")
        ));
        writeResponse(exchange, HttpStatus.OK.value(), "");
    }

    private void handleSlowRequest(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        writeResponse(exchange, HttpStatus.OK.value(), "");
    }

    private void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBody.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(responseBody);
        }
    }

    private String serverUrl(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    record CapturedRequest(String path, String accept, String contentType) {
    }

    @Configuration(proxyBeanMethods = false)
    static class InterceptorConfig {

        private static final List<URI> interceptedUris = Collections.synchronizedList(new ArrayList<>());

        @Bean
        ClientHttpRequestInterceptor testClientHttpRequestInterceptor() {
            return (request, body, execution) -> {
                interceptedUris.add(request.getURI());
                return execution.execute(request, body);
            };
        }
    }
}
