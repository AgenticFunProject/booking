package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.client.RestClientLoggingInterceptor;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilderFactory;

class IntegrationInfrastructureTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RestClientConfig.class, RestClientLoggingInterceptor.class);

    @Test
    void shouldDefineBaseIntegrationPropertiesResilienceDefaultsAndHealthExposure() throws IOException {
        PropertySource<?> properties = loadProperties("application.yml");

        assertThat(properties.getProperty("app.integration.schedule-api.base-url"))
                .isEqualTo("${SCHEDULE_API_URL:http://localhost:8082}");
        assertThat(properties.getProperty("app.integration.schedule-api.timeout-ms"))
                .isEqualTo("${SCHEDULE_API_TIMEOUT:5000}");
        assertThat(properties.getProperty("app.integration.equipment-api.base-url"))
                .isEqualTo("${EQUIPMENT_API_URL:http://localhost:8083}");
        assertThat(properties.getProperty("app.integration.equipment-api.timeout-ms"))
                .isEqualTo("${EQUIPMENT_API_TIMEOUT:5000}");
        assertThat(properties.getProperty("app.integration.quote-api.base-url"))
                .isEqualTo("${QUOTE_API_URL:http://localhost:8084}");
        assertThat(properties.getProperty("app.integration.quote-api.timeout-ms"))
                .isEqualTo("${QUOTE_API_TIMEOUT:5000}");

        assertThat(properties.getProperty("resilience4j.circuitbreaker.configs.default.slidingWindowSize"))
                .isEqualTo(10);
        assertThat(properties.getProperty("resilience4j.circuitbreaker.configs.default.failureRateThreshold"))
                .isEqualTo(50);
        assertThat(properties.getProperty("resilience4j.circuitbreaker.configs.default.waitDurationInOpenState"))
                .isEqualTo("30s");
        assertThat(properties.getProperty(
                "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState"))
                .isEqualTo(3);
        assertThat(properties.getProperty("resilience4j.retry.configs.default.maxAttempts"))
                .isEqualTo(3);
        assertThat(properties.getProperty("resilience4j.retry.configs.default.waitDuration"))
                .isEqualTo("500ms");

        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.roles"))
                .isEqualTo("ADMIN");
    }

    @Test
    void shouldCreateRestClientsWithTimeoutsJsonHeadersBaseUrlsAndLoggingInterceptor() {
        contextRunner
                .withPropertyValues(
                        "app.integration.schedule-api.base-url=https://schedule.example.test",
                        "app.integration.schedule-api.timeout-ms=1200",
                        "app.integration.equipment-api.base-url=https://equipment.example.test",
                        "app.integration.equipment-api.timeout-ms=2300",
                        "app.integration.quote-api.base-url=https://quote.example.test",
                        "app.integration.quote-api.timeout-ms=3400"
                )
                .run(context -> {
                    assertRestClientInfrastructure(
                            context.getBean("scheduleRestClient", RestClient.class),
                            "https://schedule.example.test/probe",
                            1_200);
                    assertRestClientInfrastructure(
                            context.getBean("equipmentRestClient", RestClient.class),
                            "https://equipment.example.test/probe",
                            2_300);
                    assertRestClientInfrastructure(
                            context.getBean("quoteRestClient", RestClient.class),
                            "https://quote.example.test/probe",
                            3_400);
                });
    }

    private void assertRestClientInfrastructure(RestClient restClient, String expectedProbeUri, int expectedTimeoutMs) {
        UriBuilderFactory uriBuilderFactory = getField(restClient, "uriBuilderFactory");
        HttpHeaders defaultHeaders = getField(restClient, "defaultHeaders");
        SimpleClientHttpRequestFactory requestFactory = getField(restClient, "clientRequestFactory");
        List<ClientHttpRequestInterceptor> interceptors = getField(restClient, "interceptors");

        assertThat(uriBuilderFactory.expand("/probe").toString()).isEqualTo(expectedProbeUri);
        assertThat(defaultHeaders.getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(defaultHeaders.getAccept()).containsExactly(MediaType.APPLICATION_JSON);
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(expectedTimeoutMs);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(expectedTimeoutMs);
        assertThat(interceptors).anyMatch(RestClientLoggingInterceptor.class::isInstance);
    }

    private PropertySource<?> loadProperties(String resourceName) throws IOException {
        return new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .getFirst();
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String name) {
        return (T) ReflectionTestUtils.getField(target, name);
    }
}
