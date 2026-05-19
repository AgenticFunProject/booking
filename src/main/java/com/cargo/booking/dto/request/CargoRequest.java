package com.cargo.booking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CargoRequest(
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal weightKg
) {
}
