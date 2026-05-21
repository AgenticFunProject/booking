package com.cargo.booking.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank @DefaultValue("platform-auth") String issuer,
        @NotBlank @DefaultValue("equipments-service") String audience,
        String secret,
        @NotNull @DurationMin(seconds = 1) @DefaultValue("1h") Duration expiration
) {

    public long expirationMs() {
        return expiration.toMillis();
    }
}
