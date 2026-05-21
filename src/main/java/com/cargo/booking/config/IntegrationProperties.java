package com.cargo.booking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.integration")
public record IntegrationProperties(
        @Valid @NotNull @DefaultValue ScheduleApi scheduleApi,
        @Valid @NotNull @DefaultValue EquipmentApi equipmentApi,
        @Valid @NotNull @DefaultValue QuoteApi quoteApi
) {

    public record ScheduleApi(
            @NotBlank @DefaultValue("http://localhost:8082") String baseUrl,
            @Positive @DefaultValue("5000") int timeoutMs
    ) {
    }

    public record EquipmentApi(
            @NotBlank @DefaultValue("http://localhost:8083") String baseUrl,
            @Positive @DefaultValue("5000") int timeoutMs
    ) {
    }

    public record QuoteApi(
            @NotBlank @DefaultValue("http://localhost:8084") String baseUrl,
            @Positive @DefaultValue("5000") int timeoutMs
    ) {
    }
}
