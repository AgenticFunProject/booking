package com.cargo.booking.client.dto;

import java.time.Instant;

public record ScheduleDTO(
        Long id,
        String routeName,
        Instant departureDate,
        String status
) {
}
