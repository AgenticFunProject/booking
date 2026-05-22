package com.cargo.booking.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.testutil.TestDataBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingEquipmentLineEntityTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldPassJakartaValidationForValidEquipmentLine() {
        BookingEquipmentLine equipmentLine = TestDataBuilder.anEquipmentLine().build();

        assertThat(validator.validate(equipmentLine)).isEmpty();
    }

    @Test
    void shouldRejectMissingTypeAndQuantity() {
        BookingEquipmentLine equipmentLine = BookingEquipmentLine.builder().build();

        assertThat(violationPathsFor(equipmentLine))
                .contains("type", "quantity");
    }

    @Test
    void shouldRejectQuantityBelowMinimum() {
        BookingEquipmentLine equipmentLine = BookingEquipmentLine.builder()
                .type(EquipmentType.TWENTY_FT)
                .quantity(0)
                .build();

        assertThat(violationPathsFor(equipmentLine)).contains("quantity");
    }

    @Test
    void shouldUseIdOnlyForEquality() {
        BookingEquipmentLine first = BookingEquipmentLine.builder()
                .id(7L)
                .type(EquipmentType.TWENTY_FT)
                .quantity(1)
                .build();
        BookingEquipmentLine second = BookingEquipmentLine.builder()
                .id(7L)
                .type(EquipmentType.REEFER)
                .quantity(3)
                .build();
        BookingEquipmentLine different = BookingEquipmentLine.builder()
                .id(8L)
                .type(EquipmentType.TWENTY_FT)
                .quantity(1)
                .build();

        assertThat(first)
                .isEqualTo(second)
                .isNotEqualTo(different);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldExcludeBookingFromToString() {
        Booking booking = TestDataBuilder.aBooking().build();
        BookingEquipmentLine equipmentLine = TestDataBuilder.anEquipmentLine()
                .booking(booking)
                .build();

        assertThat(equipmentLine.toString())
                .contains("type=TWENTY_FT")
                .doesNotContain("booking=");
    }

    private Set<String> violationPathsFor(BookingEquipmentLine equipmentLine) {
        return validator.validate(equipmentLine).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
