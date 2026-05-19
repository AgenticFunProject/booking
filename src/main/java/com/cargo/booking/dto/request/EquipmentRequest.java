package com.cargo.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record EquipmentRequest(
        @NotBlank String type,
        @Min(1) int quantity
) {
}
