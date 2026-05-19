package com.cargo.booking.dto.response;

import java.math.BigDecimal;

public record CargoResponse(
        String description,
        BigDecimal weightKg
) {
}
