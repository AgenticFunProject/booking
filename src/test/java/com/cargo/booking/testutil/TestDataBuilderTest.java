package com.cargo.booking.testutil;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestDataBuilderTest {

    @Test
    void shouldCreateValidBookingBuilderWithReusableDefaults() {
        Booking booking = TestDataBuilder.aBooking().build();

        assertThat(booking.getId()).isNull();
        assertThat(booking.getBookingReference()).isEqualTo("BKG-2026-00001");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getScheduleId()).isEqualTo(1001L);
        assertThat(booking.getQuoteId()).isEqualTo(2001L);
        assertThat(booking.getCustomerId()).isEqualTo(3001L);
        assertThat(booking.getCustomerName()).isEqualTo("Test Customer");
        assertThat(booking.getCustomerEmail()).isEqualTo("test@example.com");
        assertThat(booking.getCargoDescription()).isEqualTo("Test cargo");
        assertThat(booking.getCargoWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(booking.getCreatedAt()).isNull();
        assertThat(booking.getUpdatedAt()).isNull();
        assertThat(booking.getEquipmentLines()).isEmpty();
    }

    @Test
    void shouldCreatePersistedLookingBookingWhenExplicitlyRequested() {
        Booking booking = TestDataBuilder.aPersistedBooking().build();

        assertThat(booking.getId()).isEqualTo(42L);
        assertThat(booking.getCreatedAt()).isEqualTo(TestDataBuilder.DEFAULT_CREATED_AT);
        assertThat(booking.getUpdatedAt()).isEqualTo(TestDataBuilder.DEFAULT_UPDATED_AT);
    }

    @Test
    void shouldAllowStatusSpecificBookingsForLifecycleTests() {
        Booking confirmed = TestDataBuilder.aBookingWithStatus(BookingStatus.CONFIRMED).build();
        List<Booking> lifecycleBookings = TestDataBuilder.bookingsForLifecycleStates();

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(lifecycleBookings)
                .extracting(Booking::getStatus)
                .containsExactly(BookingStatus.values());
    }

    @Test
    void shouldCreateValidEquipmentLineBuilder() {
        Booking booking = TestDataBuilder.aBookingEntity();
        BookingEquipmentLine line = TestDataBuilder.anEquipmentLineFor(booking);

        assertThat(line.getType()).isEqualTo(EquipmentType.TWENTY_FT);
        assertThat(line.getQuantity()).isEqualTo(1);
        assertThat(line.getBooking()).isSameAs(booking);
    }

    @Test
    void shouldCreateValidApiRequestGraph() {
        CreateBookingRequest request = TestDataBuilder.aCreateBookingRequest();

        assertThat(request.customerId()).isEqualTo(3001L);
        assertThat(request.scheduleId()).isEqualTo(1001L);
        assertThat(request.quoteId()).isEqualTo(2001L);
        assertThat(request.customer().name()).isEqualTo("Test Customer");
        assertThat(request.customer().email()).isEqualTo("test@example.com");
        assertThat(request.cargo().description()).isEqualTo("Test cargo");
        assertThat(request.cargo().weightKg()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(request.equipment()).containsExactly(TestDataBuilder.anEquipmentRequest());
    }

    @Test
    void shouldCreateValidServiceRequestWithoutConfusingItWithApiDto() {
        com.cargo.booking.service.CreateBookingRequest request =
                TestDataBuilder.aServiceCreateBookingRequest();

        assertThat(request.customerId()).isEqualTo(3001L);
        assertThat(request.customerName()).isEqualTo("Test Customer");
        assertThat(request.cargoWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(request.equipment())
                .containsExactly(new EquipmentLineRequest("20FT", 1));
    }
}
