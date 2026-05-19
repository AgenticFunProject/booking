package com.cargo.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateBookingRequest(
        @NotNull Long customerId,
        @NotNull Long scheduleId,
        @NotNull Long quoteId,
        @NotNull @Valid CustomerRequest customer,
        @NotNull @Valid CargoRequest cargo,
        @NotEmpty @Valid List<@Valid EquipmentRequest> equipment
) {
}
