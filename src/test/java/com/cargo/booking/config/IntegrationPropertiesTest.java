package com.cargo.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class IntegrationPropertiesTest {

    private final ApplicationContextRunner propertiesRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void shouldBindDefaultIntegrationValues() {
        propertiesRunner.run(context -> {
            IntegrationProperties properties = context.getBean(IntegrationProperties.class);

            assertThat(properties.scheduleApi().baseUrl()).isEqualTo("http://localhost:8082");
            assertThat(properties.scheduleApi().timeoutMs()).isEqualTo(5_000);
            assertThat(properties.equipmentApi().baseUrl()).isEqualTo("http://localhost:8083");
            assertThat(properties.equipmentApi().timeoutMs()).isEqualTo(5_000);
            assertThat(properties.quoteApi().baseUrl()).isEqualTo("http://localhost:8084");
            assertThat(properties.quoteApi().timeoutMs()).isEqualTo(5_000);
        });
    }

    @Test
    void shouldBindExternalizedIntegrationValues() {
        propertiesRunner
                .withPropertyValues(
                        "app.integration.schedule-api.base-url=https://schedule.example.test",
                        "app.integration.schedule-api.timeout-ms=2500",
                        "app.integration.equipment-api.base-url=https://equipment.example.test",
                        "app.integration.equipment-api.timeout-ms=3000",
                        "app.integration.quote-api.base-url=https://quote.example.test",
                        "app.integration.quote-api.timeout-ms=3500"
                )
                .run(context -> {
                    IntegrationProperties properties = context.getBean(IntegrationProperties.class);

                    assertThat(properties.scheduleApi().baseUrl()).isEqualTo("https://schedule.example.test");
                    assertThat(properties.scheduleApi().timeoutMs()).isEqualTo(2_500);
                    assertThat(properties.equipmentApi().baseUrl()).isEqualTo("https://equipment.example.test");
                    assertThat(properties.equipmentApi().timeoutMs()).isEqualTo(3_000);
                    assertThat(properties.quoteApi().baseUrl()).isEqualTo("https://quote.example.test");
                    assertThat(properties.quoteApi().timeoutMs()).isEqualTo(3_500);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(IntegrationProperties.class)
    static class PropertiesConfig {
    }
}
