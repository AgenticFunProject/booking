package com.cargo.booking.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.dto.response.BookingResponse;
import com.cargo.booking.dto.response.EquipmentResponse;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingMapperTest {

    private final BookingMapper mapper = new BookingMapper();

    @Test
    void shouldMapBookingToResponse() {
        Booking booking = bookingWithEquipment();

        BookingResponse response = mapper.toResponse(booking);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.bookingReference()).isEqualTo("BKG-2026-00042");
        assertThat(response.customerId()).isEqualTo(3001L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.scheduleId()).isEqualTo(1001L);
        assertThat(response.quoteId()).isEqualTo(2001L);
        assertThat(response.customer().name()).isEqualTo("Acme Shipping Co.");
        assertThat(response.customer().email()).isEqualTo("logistics@acme.com");
        assertThat(response.customer().phone()).isEqualTo("+36-1-234-5678");
        assertThat(response.cargo().description()).isEqualTo("Industrial machinery parts");
        assertThat(response.cargo().weightKg()).isEqualByComparingTo("12000.00");
        assertThat(response.equipment())
                .containsExactly(
                        new EquipmentResponse("20FT", 2),
                        new EquipmentResponse("40HC", 1)
                );
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-05-18T10:15:30Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-05-18T10:20:45Z"));
    }

    @Test
    void shouldMapBookingToCreatedResponse() {
        Booking booking = bookingWithEquipment();

        BookingCreatedResponse response = mapper.toCreatedResponse(booking);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.bookingReference()).isEqualTo("BKG-2026-00042");
        assertThat(response.customerId()).isEqualTo(3001L);
        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-05-18T10:15:30Z"));
    }

    @Test
    void shouldMapEquipmentTypeUsingApiCode() {
        BookingEquipmentLine equipmentLine = BookingEquipmentLine.builder()
                .type(EquipmentType.REEFER)
                .quantity(3)
                .build();

        EquipmentResponse response = mapper.toEquipmentResponse(equipmentLine);

        assertThat(response).isEqualTo(new EquipmentResponse("REEFER", 3));
    }

    @Test
    void shouldMapNullEquipmentLinesToEmptyList() {
        Booking booking = bookingWithEquipment();
        booking.setEquipmentLines(null);

        BookingResponse response = mapper.toResponse(booking);

        assertThat(response.equipment()).isEmpty();
    }

    private Booking bookingWithEquipment() {
        Booking booking = Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .status(BookingStatus.CONFIRMED)
                .customerId(3001L)
                .scheduleId(1001L)
                .quoteId(2001L)
                .customerName("Acme Shipping Co.")
                .customerEmail("logistics@acme.com")
                .customerPhone("+36-1-234-5678")
                .cargoDescription("Industrial machinery parts")
                .cargoWeightKg(new BigDecimal("12000.00"))
                .createdAt(Instant.parse("2026-05-18T10:15:30Z"))
                .updatedAt(Instant.parse("2026-05-18T10:20:45Z"))
                .build();

        BookingEquipmentLine twentyFtLine = BookingEquipmentLine.builder()
                .type(EquipmentType.TWENTY_FT)
                .quantity(2)
                .booking(booking)
                .build();
        BookingEquipmentLine fortyHcLine = BookingEquipmentLine.builder()
                .type(EquipmentType.FORTY_HC)
                .quantity(1)
                .booking(booking)
                .build();
        booking.setEquipmentLines(List.of(twentyFtLine, fortyHcLine));

        return booking;
    }
}
