package com.cargo.booking.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorResponseBuilderTest {

    @Test
    void shouldBuildErrorResponseFromStatusAndRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings/BKG-2026-00042");
        request.addHeader(ErrorResponseBuilder.REQUEST_ID_HEADER, "req-123");

        ErrorResponse response = ErrorResponseBuilder.buildError(
                HttpStatus.NOT_FOUND,
                "Booking not found with reference BKG-2026-00042",
                request);

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Booking not found with reference BKG-2026-00042");
        assertThat(response.path()).isEqualTo("/api/v1/bookings/BKG-2026-00042");
        assertThat(response.requestId()).isEqualTo("req-123");
    }

    @Test
    void shouldOmitBlankRequestIdWhenBuildingErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        request.addHeader(ErrorResponseBuilder.REQUEST_ID_HEADER, " ");

        ErrorResponse response = ErrorResponseBuilder.buildError(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request body",
                request);

        assertThat(response.requestId()).isNull();
    }

    @Test
    void shouldBuildValidationErrorResponseWithViolations() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        List<FieldViolation> violations = List.of(
                new FieldViolation("customer.email", "must be a well-formed email address", "not-an-email"));

        ValidationErrorResponse response = ErrorResponseBuilder.buildValidationError(
                HttpStatus.BAD_REQUEST,
                "Validation failed with 1 error(s)",
                violations,
                request);

        assertThat(response.timestamp()).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("Validation failed with 1 error(s)");
        assertThat(response.path()).isEqualTo("/api/v1/bookings");
        assertThat(response.requestId()).isNull();
        assertThat(response.violations()).containsExactlyElementsOf(violations);
    }
}
