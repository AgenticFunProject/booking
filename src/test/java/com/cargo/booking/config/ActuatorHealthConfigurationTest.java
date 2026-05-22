package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ActuatorHealthConfigurationTest {

    @Test
    void shouldExposeBaseActuatorEndpointsWithAdminHealthDetails() throws IOException {
        PropertySource<?> properties = loadProperties("application.yml");

        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.roles"))
                .isEqualTo("ADMIN");
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                .isEqualTo("30s");
    }

    @Test
    void shouldShowLocalHealthDetailsForStubbedDevelopment() throws IOException {
        PropertySource<?> properties = loadProperties("application-local.yml");

        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/booking_db}");
        assertThat(properties.getProperty("spring.datasource.username"))
                .isEqualTo("${DB_USERNAME:booking_user}");
        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${DB_PASSWORD:booking_pass}");
        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("app.integration.schedule-api.base-url"))
                .isEqualTo("${SCHEDULE_API_URL:http://localhost:8082}");
        assertThat(properties.getProperty("app.integration.equipment-api.base-url"))
                .isEqualTo("${EQUIPMENT_API_URL:http://localhost:8083}");
        assertThat(properties.getProperty("app.integration.quote-api.base-url"))
                .isEqualTo("${QUOTE_API_URL:http://localhost:8084}");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("always");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("always");
        assertThat(properties.getProperty("logging.level.root")).isEqualTo("INFO");
        assertThat(properties.getProperty("logging.level.com.cargo.booking")).isEqualTo("DEBUG");
    }

    @Test
    void shouldKeepDevHealthDetailsAdminOnly() throws IOException {
        PropertySource<?> properties = loadProperties("application-dev.yml");

        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/booking_db}");
        assertThat(properties.getProperty("spring.datasource.username"))
                .isEqualTo("${DB_USERNAME:booking_user}");
        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${DB_PASSWORD:booking_pass}");
        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("app.security.jwt.secret"))
                .isEqualTo("${JWT_SECRET:${AUTH_JWT_SECRET:dev-secret-key-that-is-at-least-256-bits-long-for-hs256}}");
        assertThat(properties.getProperty("app.integration.schedule-api.base-url"))
                .isEqualTo("${SCHEDULE_API_URL:https://schedule-api.dev.cargo.internal}");
        assertThat(properties.getProperty("app.integration.equipment-api.base-url"))
                .isEqualTo("${EQUIPMENT_API_URL:https://equipment-api.dev.cargo.internal}");
        assertThat(properties.getProperty("app.integration.quote-api.base-url"))
                .isEqualTo("${QUOTE_API_URL:https://quote-api.dev.cargo.internal}");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("when_authorized");
        assertThat(properties.getProperty("management.endpoint.health.roles"))
                .isEqualTo("ADMIN");
        assertThat(properties.getProperty("logging.level.root")).isEqualTo("INFO");
        assertThat(properties.getProperty("logging.level.com.cargo.booking")).isEqualTo("INFO");
    }

    @Test
    void shouldHideProductionHealthDetailsAndMetrics() throws IOException {
        PropertySource<?> properties = loadProperties("application-prod.yml");

        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("${SPRING_DATASOURCE_URL}");
        assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(properties.getProperty("spring.jpa.show-sql")).isEqualTo(false);
        assertThat(properties.getProperty("spring.jpa.open-in-view")).isEqualTo(false);
        assertThat(properties.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(properties.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("app.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(properties.getProperty("app.integration.schedule-api.base-url"))
                .isEqualTo("${SCHEDULE_API_URL:https://schedule-api.prod.cargo.internal}");
        assertThat(properties.getProperty("app.integration.equipment-api.base-url"))
                .isEqualTo("${EQUIPMENT_API_URL:https://equipment-api.prod.cargo.internal}");
        assertThat(properties.getProperty("app.integration.quote-api.base-url"))
                .isEqualTo("${QUOTE_API_URL:https://quote-api.prod.cargo.internal}");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("never");
        assertThat(properties.getProperty("logging.level.root")).isEqualTo("WARN");
        assertThat(properties.getProperty("logging.level.com.cargo.booking")).isEqualTo("INFO");
    }

    @Test
    void shouldConfigureTestProfileForEmbeddedPostgresAndJwtTests() throws IOException {
        PropertySource<?> properties = loadProperties("application-test.yml");

        assertThat(properties.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("spring.datasource.url")).isNull();
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("app.security.jwt.issuer")).isEqualTo("test-issuer");
        assertThat(properties.getProperty("app.security.jwt.audience")).isEqualTo("equipments-service");
        assertThat(properties.getProperty("app.security.jwt.secret"))
                .isEqualTo("${AUTH_JWT_SECRET:test-secret-key-that-is-at-least-256-bits-long}");
        assertThat(properties.getProperty("app.integration.schedule-api.timeout-ms")).isEqualTo("${SCHEDULE_API_TIMEOUT:1000}");
        assertThat(properties.getProperty("app.integration.equipment-api.timeout-ms")).isEqualTo("${EQUIPMENT_API_TIMEOUT:1000}");
        assertThat(properties.getProperty("app.integration.quote-api.timeout-ms")).isEqualTo("${QUOTE_API_TIMEOUT:1000}");
    }

    private PropertySource<?> loadProperties(String resourceName) throws IOException {
        return new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .getFirst();
    }
}
