package com.cargo.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class SecurityPropertiesTest {

    private final ApplicationContextRunner propertiesRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class);

    private final ApplicationContextRunner validatingRunner = new ApplicationContextRunner()
            .withUserConfiguration(ValidatingConfig.class);

    @Test
    void shouldBindDefaultSecurityAndJwtValues() {
        propertiesRunner.run(context -> {
            SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
            JwtProperties jwtProperties = context.getBean(JwtProperties.class);

            assertThat(securityProperties.enabled()).isTrue();
            assertThat(jwtProperties.issuer()).isEqualTo("cargo-platform");
            assertThat(jwtProperties.audience()).isEqualTo("equipments-service");
            assertThat(jwtProperties.secret()).isEqualTo("default-dev-secret-key-min-256-bits-long-for-hs256");
            assertThat(jwtProperties.expiration()).isEqualTo(Duration.ofHours(1));
            assertThat(jwtProperties.expirationMs()).isEqualTo(3_600_000L);
        });
    }

    @Test
    void shouldBindExternalizedSecurityAndJwtValues() {
        propertiesRunner
                .withPropertyValues(
                        "app.security.enabled=false",
                        "app.security.jwt.issuer=users-service",
                        "app.security.jwt.audience=booking-service",
                        "app.security.jwt.secret=external-secret-that-is-long-enough",
                        "app.security.jwt.expiration-ms=1800000"
                )
                .run(context -> {
                    SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
                    JwtProperties jwtProperties = context.getBean(JwtProperties.class);

                    assertThat(securityProperties.enabled()).isFalse();
                    assertThat(jwtProperties.issuer()).isEqualTo("users-service");
                    assertThat(jwtProperties.audience()).isEqualTo("booking-service");
                    assertThat(jwtProperties.secret()).isEqualTo("external-secret-that-is-long-enough");
                    assertThat(jwtProperties.expirationMs()).isEqualTo(1_800_000L);
                });
    }

    @Test
    void shouldRejectBlankSecretWhenSecurityIsEnabled() {
        validatingRunner
                .withPropertyValues(
                        "app.security.enabled=true",
                        "app.security.jwt.secret="
                )
                .run(context -> assertThat(context.getStartupFailure())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(
                                "app.security.jwt.secret must be configured when app.security.enabled is true"
                        ));
    }

    @Test
    void shouldRejectShortSecretWhenSecurityIsEnabled() {
        validatingRunner
                .withPropertyValues(
                        "app.security.enabled=true",
                        "app.security.jwt.secret=too-short"
                )
                .run(context -> assertThat(context.getStartupFailure())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("app.security.jwt.secret must be at least 32 characters for HS256"));
    }

    @Test
    void shouldBindDefaultSecretWhenSecurityIsDisabled() {
        validatingRunner
                .withPropertyValues("app.security.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).secret())
                            .isEqualTo("default-dev-secret-key-min-256-bits-long-for-hs256");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
    static class PropertiesConfig {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
    static class ValidatingConfig {

        @Bean
        JwtPropertiesValidator jwtPropertiesValidator(
                SecurityProperties securityProperties,
                JwtProperties jwtProperties
        ) {
            return new JwtPropertiesValidator(securityProperties, jwtProperties);
        }
    }
}
