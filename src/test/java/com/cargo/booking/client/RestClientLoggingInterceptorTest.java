package com.cargo.booking.client;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

class RestClientLoggingInterceptorTest {

    private final RestClientLoggingInterceptor interceptor = new RestClientLoggingInterceptor();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RestClientLoggingInterceptor.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    @Test
    void shouldLogRequestMetadataRedactedHeadersAndResponseTimingWithoutBodies() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer secret-token");
        headers.add("X-Request-ID", "req-123");
        HttpRequest request = new TestHttpRequest(HttpMethod.POST, URI.create("https://example.test/bookings"), headers);
        byte[] body = "sensitive request body".getBytes(StandardCharsets.UTF_8);

        ClientHttpResponse response = interceptor.intercept(
                request,
                body,
                (executedRequest, executedBody) -> new TestClientHttpResponse(HttpStatus.ACCEPTED, "sensitive response body")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(loggedMessages())
                .contains("Outbound request: POST https://example.test/bookings")
                .anyMatch(message -> message.contains("Outbound request headers:")
                        && message.contains("X-Request-ID")
                        && message.contains("req-123")
                        && message.contains("[REDACTED]"))
                .anyMatch(message -> message.matches("Outbound response: 202 in \\d+ms"));
        assertThat(loggedMessages())
                .noneMatch(message -> message.contains("Bearer secret-token"))
                .noneMatch(message -> message.contains("sensitive request body"))
                .noneMatch(message -> message.contains("sensitive response body"));
    }

    private java.util.List<String> loggedMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static final class TestHttpRequest implements HttpRequest {

        private final HttpMethod method;
        private final URI uri;
        private final HttpHeaders headers;

        private TestHttpRequest(HttpMethod method, URI uri, HttpHeaders headers) {
            this.method = method;
            this.uri = uri;
            this.headers = headers;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Map.of();
        }
    }

    private record TestClientHttpResponse(HttpStatus status, String body) implements ClientHttpResponse {

        @Override
        public HttpStatus getStatusCode() {
            return status;
        }

        @Override
        public String getStatusText() {
            return status.getReasonPhrase();
        }

        @Override
        public void close() {
        }

        @Override
        public ByteArrayInputStream getBody() {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }
}
