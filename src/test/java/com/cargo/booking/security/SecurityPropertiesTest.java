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
            assertThat(jwtProperties.issuer()).isEqualTo("platform-auth");
            assertThat(jwtProperties.audience()).isEqualTo("equipments-service");
            assertThat(jwtProperties.secret()).isNull();
            assertThat(jwtProperties.expiration()).isEqualTo(Duration.ofHours(1));
            assertThat(jwtProperties.expirationMs()).isEqualTo(3_600_000);
        });
    }

    @Test
    void shouldBindExternalizedSecurityAndJwtValues() {
        propertiesRunner
                .withPropertyValues(
                        "app.security.enabled=false",
                        "app.jwt.issuer=users-service",
                        "app.jwt.audience=booking-service",
                        "app.jwt.secret=external-secret-that-is-long-enough",
                        "app.jwt.expiration=30m"
                )
                .run(context -> {
                    SecurityProperties securityProperties = context.getBean(SecurityProperties.class);
                    JwtProperties jwtProperties = context.getBean(JwtProperties.class);

                    assertThat(securityProperties.enabled()).isFalse();
                    assertThat(jwtProperties.issuer()).isEqualTo("users-service");
                    assertThat(jwtProperties.audience()).isEqualTo("booking-service");
                    assertThat(jwtProperties.secret()).isEqualTo("external-secret-that-is-long-enough");
                    assertThat(jwtProperties.expirationMs()).isEqualTo(1_800_000);
                });
    }

    @Test
    void shouldRejectMissingSecretWhenSecurityIsEnabled() {
        validatingRunner
                .withPropertyValues("app.security.enabled=true")
                .run(context -> assertThat(context.getStartupFailure())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(
                                "app.jwt.secret must be configured when app.security.enabled is true"
                        ));
    }

    @Test
    void shouldRejectShortSecretWhenSecurityIsEnabled() {
        validatingRunner
                .withPropertyValues(
                        "app.security.enabled=true",
                        "app.jwt.secret=too-short"
                )
                .run(context -> assertThat(context.getStartupFailure())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("app.jwt.secret must be at least 32 characters for HS256"));
    }

    @Test
    void shouldAllowMissingSecretWhenSecurityIsDisabled() {
        validatingRunner
                .withPropertyValues("app.security.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).secret()).isNull();
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
