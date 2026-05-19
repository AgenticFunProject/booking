package com.cargo.booking.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CreateBookingRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldValidateValidCreateBookingRequest() {
        CreateBookingRequest request = validRequest();

        Set<ConstraintViolation<CreateBookingRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldReportScalarNestedAndEquipmentViolations() {
        CreateBookingRequest request = new CreateBookingRequest(
                null,
                null,
                null,
                new CustomerRequest("", "not-an-email", "1".repeat(51)),
                new CargoRequest("", BigDecimal.ZERO),
                List.of(new EquipmentRequest("", 0))
        );

        Set<String> violationPaths = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violationPaths).contains(
                "customerId",
                "scheduleId",
                "quoteId",
                "customer.name",
                "customer.email",
                "customer.phone",
                "cargo.description",
                "cargo.weightKg",
                "equipment[0].type",
                "equipment[0].quantity"
        );
    }

    @Test
    void shouldRequireNestedObjectsAndEquipmentList() {
        CreateBookingRequest request = new CreateBookingRequest(
                3001L,
                1001L,
                2001L,
                null,
                null,
                List.of()
        );

        Set<String> violationPaths = validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violationPaths).contains("customer", "cargo", "equipment");
    }

    private CreateBookingRequest validRequest() {
        return new CreateBookingRequest(
                3001L,
                1001L,
                2001L,
                new CustomerRequest("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoRequest("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(
                        new EquipmentRequest("20FT", 2),
                        new EquipmentRequest("40HC", 1)
                )
        );
    }
}
