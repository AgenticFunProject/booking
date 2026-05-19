package com.cargo.booking.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldSerializeErrorResponseFieldsAndOmitAbsentRequestId() throws Exception {
        ErrorResponse response = new ErrorResponse(
                Instant.parse("2026-04-01T10:00:00Z"),
                404,
                "Not Found",
                "Booking not found with reference BKG-2026-00042",
                "/api/v1/bookings/BKG-2026-00042",
                null);

        String json = jsonMapper.writeValueAsString(response);

        assertThat(json).contains("\"timestamp\":\"2026-04-01T10:00:00Z\"");
        assertThat(json).contains("\"status\":404");
        assertThat(json).contains("\"error\":\"Not Found\"");
        assertThat(json).contains("\"message\":\"Booking not found with reference BKG-2026-00042\"");
        assertThat(json).contains("\"path\":\"/api/v1/bookings/BKG-2026-00042\"");
        assertThat(json).doesNotContain("requestId");
    }

    @Test
    void shouldSerializeValidationResponseWithRequestIdAndViolations() throws Exception {
        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.parse("2026-04-01T10:00:00Z"),
                400,
                "Bad Request",
                "Validation failed with 1 error(s)",
                "/api/v1/bookings",
                "req-123",
                List.of(new FieldViolation("customer.email", "must be a well-formed email address", "not-an-email")));

        String json = jsonMapper.writeValueAsString(response);

        assertThat(json).contains("\"requestId\":\"req-123\"");
        assertThat(json).contains("\"violations\":[");
        assertThat(json).contains("\"field\":\"customer.email\"");
        assertThat(json).contains("\"message\":\"must be a well-formed email address\"");
        assertThat(json).contains("\"rejectedValue\":\"not-an-email\"");
    }
}
