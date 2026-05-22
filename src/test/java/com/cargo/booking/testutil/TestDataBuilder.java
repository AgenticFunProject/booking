package com.cargo.booking.testutil;

import com.cargo.booking.dto.request.CargoRequest;
import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.CustomerRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public final class TestDataBuilder {

    public static final Long DEFAULT_BOOKING_ID = 42L;
    public static final String DEFAULT_BOOKING_REFERENCE = "BKG-2026-00001";
    public static final BookingStatus DEFAULT_STATUS = BookingStatus.PENDING;
    public static final Long DEFAULT_SCHEDULE_ID = 1001L;
    public static final Long DEFAULT_QUOTE_ID = 2001L;
    public static final Long DEFAULT_CUSTOMER_ID = 3001L;
    public static final String DEFAULT_CUSTOMER_NAME = "Test Customer";
    public static final String DEFAULT_CUSTOMER_EMAIL = "test@example.com";
    public static final String DEFAULT_CUSTOMER_PHONE = "+1-555-0100";
    public static final String DEFAULT_CARGO_DESCRIPTION = "Test cargo";
    public static final BigDecimal DEFAULT_CARGO_WEIGHT_KG = BigDecimal.valueOf(1000);
    public static final String DEFAULT_EQUIPMENT_TYPE = "20FT";
    public static final int DEFAULT_EQUIPMENT_QUANTITY = 1;
    public static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    public static final Instant DEFAULT_UPDATED_AT = Instant.parse("2026-01-01T00:05:00Z");

    private TestDataBuilder() {
    }

    public static Booking.BookingBuilder aBooking() {
        return Booking.builder()
                .id(DEFAULT_BOOKING_ID)
                .bookingReference(DEFAULT_BOOKING_REFERENCE)
                .status(DEFAULT_STATUS)
                .scheduleId(DEFAULT_SCHEDULE_ID)
                .quoteId(DEFAULT_QUOTE_ID)
                .customerId(DEFAULT_CUSTOMER_ID)
                .customerName(DEFAULT_CUSTOMER_NAME)
                .customerEmail(DEFAULT_CUSTOMER_EMAIL)
                .customerPhone(DEFAULT_CUSTOMER_PHONE)
                .cargoDescription(DEFAULT_CARGO_DESCRIPTION)
                .cargoWeightKg(DEFAULT_CARGO_WEIGHT_KG)
                .createdAt(DEFAULT_CREATED_AT)
                .updatedAt(DEFAULT_UPDATED_AT);
    }

    public static Booking.BookingBuilder aBookingWithStatus(BookingStatus status) {
        return aBooking().status(status);
    }

    public static Booking aBookingEntity() {
        return aBooking().build();
    }

    public static Booking aBookingEntityWithStatus(BookingStatus status) {
        return aBookingWithStatus(status).build();
    }

    public static List<Booking> bookingsForLifecycleStates() {
        return Arrays.stream(BookingStatus.values())
                .map(TestDataBuilder::aBookingEntityWithStatus)
                .toList();
    }

    public static BookingEquipmentLine.BookingEquipmentLineBuilder anEquipmentLine() {
        return BookingEquipmentLine.builder()
                .type(EquipmentType.fromCode(DEFAULT_EQUIPMENT_TYPE))
                .quantity(DEFAULT_EQUIPMENT_QUANTITY);
    }

    public static BookingEquipmentLine anEquipmentLineFor(Booking booking) {
        return anEquipmentLine()
                .booking(booking)
                .build();
    }

    public static CreateBookingRequest aCreateBookingRequest() {
        return new CreateBookingRequest(
                DEFAULT_CUSTOMER_ID,
                DEFAULT_SCHEDULE_ID,
                DEFAULT_QUOTE_ID,
                aCustomerRequest(),
                aCargoRequest(),
                List.of(anEquipmentRequest())
        );
    }

    public static com.cargo.booking.service.CreateBookingRequest aServiceCreateBookingRequest() {
        return new com.cargo.booking.service.CreateBookingRequest(
                DEFAULT_CUSTOMER_ID,
                DEFAULT_SCHEDULE_ID,
                DEFAULT_QUOTE_ID,
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                DEFAULT_CUSTOMER_PHONE,
                DEFAULT_CARGO_DESCRIPTION,
                DEFAULT_CARGO_WEIGHT_KG,
                List.of(aServiceEquipmentLineRequest())
        );
    }

    public static CustomerRequest aCustomerRequest() {
        return new CustomerRequest(
                DEFAULT_CUSTOMER_NAME,
                DEFAULT_CUSTOMER_EMAIL,
                DEFAULT_CUSTOMER_PHONE
        );
    }

    public static CargoRequest aCargoRequest() {
        return new CargoRequest(
                DEFAULT_CARGO_DESCRIPTION,
                DEFAULT_CARGO_WEIGHT_KG
        );
    }

    public static EquipmentRequest anEquipmentRequest() {
        return new EquipmentRequest(
                DEFAULT_EQUIPMENT_TYPE,
                DEFAULT_EQUIPMENT_QUANTITY
        );
    }

    public static EquipmentLineRequest aServiceEquipmentLineRequest() {
        return new EquipmentLineRequest(
                DEFAULT_EQUIPMENT_TYPE,
                DEFAULT_EQUIPMENT_QUANTITY
        );
    }
}
