package com.cargo.booking.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @DefaultValue("cargo-platform") String issuer,
        @NotBlank @DefaultValue("equipments-service") String audience,
        @DefaultValue("default-dev-secret-key-min-256-bits-long-for-hs256") String secret,
        @NotNull @Min(1) @DefaultValue("3600000") Long expirationMs
) {

    public Duration expiration() {
        return Duration.ofMillis(expirationMs);
    }
}
