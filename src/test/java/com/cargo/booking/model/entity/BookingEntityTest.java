package com.cargo.booking.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.testutil.TestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingEntityTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldInitializeStatusAndEquipmentLinesWhenBuiltWithDefaults() {
        Booking booking = Booking.builder().build();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getEquipmentLines()).isEmpty();
    }

    @Test
    void shouldAllowEquipmentLinesToReferenceParentBooking() {
        Booking booking = TestDataBuilder.aBooking().build();
        BookingEquipmentLine equipmentLine = BookingEquipmentLine.builder()
                .type(EquipmentType.FORTY_HC)
                .quantity(2)
                .booking(booking)
                .build();

        booking.getEquipmentLines().add(equipmentLine);

        assertThat(booking.getEquipmentLines()).containsExactly(equipmentLine);
        assertThat(equipmentLine.getBooking()).isSameAs(booking);
    }

    @Test
    void shouldPassJakartaValidationForValidBooking() {
        Booking booking = TestDataBuilder.aBooking().build();

        assertThat(validator.validate(booking)).isEmpty();
    }

    @Test
    void shouldRejectMissingRequiredBookingFields() {
        Booking booking = TestDataBuilder.aBooking()
                .bookingReference(" ")
                .status(null)
                .scheduleId(null)
                .quoteId(null)
                .customerId(null)
                .customerName("")
                .customerEmail(null)
                .cargoDescription(" ")
                .cargoWeightKg(null)
                .build();

        assertThat(violationPathsFor(booking))
                .contains(
                        "bookingReference",
                        "status",
                        "scheduleId",
                        "quoteId",
                        "customerId",
                        "customerName",
                        "customerEmail",
                        "cargoDescription",
                        "cargoWeightKg"
                );
    }

    @Test
    void shouldRejectInvalidBookingFieldFormatsAndBounds() {
        Booking booking = TestDataBuilder.aBooking()
                .customerName("x".repeat(256))
                .customerEmail("not-an-email")
                .customerPhone("1".repeat(51))
                .cargoDescription("x".repeat(1001))
                .cargoWeightKg(BigDecimal.ZERO)
                .build();

        assertThat(violationPathsFor(booking))
                .contains(
                        "customerName",
                        "customerEmail",
                        "customerPhone",
                        "cargoDescription",
                        "cargoWeightKg"
                );
    }

    @Test
    void shouldUseIdOnlyForEquality() {
        Booking first = TestDataBuilder.aBooking()
                .id(42L)
                .bookingReference("BKG-2026-00001")
                .customerName("Original Customer")
                .build();
        Booking second = TestDataBuilder.aBooking()
                .id(42L)
                .bookingReference("BKG-2026-99999")
                .customerName("Renamed Customer")
                .build();
        Booking different = TestDataBuilder.aBooking()
                .id(43L)
                .bookingReference("BKG-2026-00002")
                .build();

        assertThat(first)
                .isEqualTo(second)
                .isNotEqualTo(different);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldExcludeEquipmentLinesFromToString() {
        Booking booking = TestDataBuilder.aBooking().build();
        BookingEquipmentLine equipmentLine = TestDataBuilder.anEquipmentLineFor(booking);
        booking.getEquipmentLines().add(equipmentLine);

        assertThat(booking.toString())
                .contains("bookingReference=BKG-2026-00001")
                .doesNotContain("equipmentLines");
    }

    private Set<String> violationPathsFor(Booking booking) {
        return validator.validate(booking).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
