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
    }

    @Test
    void shouldShowLocalHealthDetailsForStubbedDevelopment() throws IOException {
        PropertySource<?> properties = loadProperties("application-local.yml");

        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(false);
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("always");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("always");
    }

    @Test
    void shouldKeepDevHealthDetailsAdminOnly() throws IOException {
        PropertySource<?> properties = loadProperties("application-dev.yml");

        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(true);
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
    void shouldHideProductionHealthDetailsAndMetrics() throws IOException {
        PropertySource<?> properties = loadProperties("application-prod.yml");

        assertThat(properties.getProperty("app.security.enabled")).isEqualTo(true);
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info");
        assertThat(properties.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
        assertThat(properties.getProperty("management.endpoint.health.show-components"))
                .isEqualTo("never");
    }

    private PropertySource<?> loadProperties(String resourceName) throws IOException {
        return new YamlPropertySourceLoader()
                .load(resourceName, new ClassPathResource(resourceName))
                .getFirst();
    }
}
