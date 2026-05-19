package com.cargo.booking.mapper;

import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.dto.response.BookingResponse;
import com.cargo.booking.dto.response.CargoResponse;
import com.cargo.booking.dto.response.CustomerResponse;
import com.cargo.booking.dto.response.EquipmentResponse;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking entity) {
        Objects.requireNonNull(entity, "booking must not be null");

        return new BookingResponse(
                entity.getId(),
                entity.getBookingReference(),
                entity.getCustomerId(),
                entity.getStatus().name(),
                entity.getScheduleId(),
                entity.getQuoteId(),
                new CustomerResponse(
                        entity.getCustomerName(),
                        entity.getCustomerEmail(),
                        entity.getCustomerPhone()
                ),
                new CargoResponse(
                        entity.getCargoDescription(),
                        entity.getCargoWeightKg()
                ),
                toEquipmentResponses(entity.getEquipmentLines()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BookingCreatedResponse toCreatedResponse(Booking entity) {
        Objects.requireNonNull(entity, "booking must not be null");

        return new BookingCreatedResponse(
                entity.getId(),
                entity.getBookingReference(),
                entity.getCustomerId(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }

    public EquipmentResponse toEquipmentResponse(BookingEquipmentLine entity) {
        Objects.requireNonNull(entity, "equipment line must not be null");

        return new EquipmentResponse(
                entity.getType().getCode(),
                entity.getQuantity()
        );
    }

    private List<EquipmentResponse> toEquipmentResponses(List<BookingEquipmentLine> equipmentLines) {
        if (equipmentLines == null) {
            return List.of();
        }

        return equipmentLines.stream()
                .map(this::toEquipmentResponse)
                .toList();
    }
}
