package com.cargo.booking.dto.response;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingReference,
        Long customerId,
        String status,
        Long scheduleId,
        Long quoteId,
        CustomerResponse customer,
        CargoResponse cargo,
        List<EquipmentResponse> equipment,
        Instant createdAt,
        Instant updatedAt
) {

    public BookingResponse {
        equipment = List.copyOf(equipment);
    }
}
