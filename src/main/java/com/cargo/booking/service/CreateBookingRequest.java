package com.cargo.booking.service;

import java.math.BigDecimal;
import java.util.List;

public record CreateBookingRequest(
        Long customerId,
        Long scheduleId,
        Long quoteId,
        String customerName,
        String customerEmail,
        String customerPhone,
        String cargoDescription,
        BigDecimal cargoWeightKg,
        List<EquipmentLineRequest> equipment
) {

    public record EquipmentLineRequest(String type, int quantity) {
    }
}
