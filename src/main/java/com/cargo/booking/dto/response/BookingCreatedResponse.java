package com.cargo.booking.dto.response;

import java.time.Instant;

public record BookingCreatedResponse(
        Long id,
        String bookingReference,
        Long customerId,
        String status,
        Instant createdAt
) {
}
