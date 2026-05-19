package com.cargo.booking.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapBookingNotFoundToNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings/BKG-2026-00042");

        ResponseEntity<ErrorResponse> response = handler.handleBookingNotFound(
                new BookingNotFoundException("Booking not found with reference BKG-2026-00042"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.NOT_FOUND,
                "Booking not found with reference BKG-2026-00042",
                "/api/v1/bookings/BKG-2026-00042");
    }

    @Test
    void shouldMapBookingValidationToBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings/not-a-booking");

        ResponseEntity<ErrorResponse> response = handler.handleBookingValidation(
                new BookingValidationException("Invalid booking identifier: not-a-booking"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                "Invalid booking identifier: not-a-booking",
                "/api/v1/bookings/not-a-booking");
    }

    @Test
    void shouldMapIllegalStateTransitionToConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/bookings/42/cancel");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateTransition(
                new IllegalStateTransitionException("Cannot transition booking from COMPLETED to CANCELLED"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.CONFLICT,
                "Cannot transition booking from COMPLETED to CANCELLED",
                "/api/v1/bookings/42/cancel");
    }

    @Test
    void shouldMapScheduleNotAvailableToUnprocessableEntity() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");

        ResponseEntity<ErrorResponse> response = handler.handleScheduleNotAvailable(
                new ScheduleNotAvailableException("Schedule is not available for id: 1001"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Schedule is not available for id: 1001",
                "/api/v1/bookings");
    }

    @Test
    void shouldMapQuoteNotValidToUnprocessableEntity() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");

        ResponseEntity<ErrorResponse> response = handler.handleQuoteNotValid(
                new QuoteNotValidException("Quote is not valid for quoteId 2001 and scheduleId 1001"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Quote is not valid for quoteId 2001 and scheduleId 1001",
                "/api/v1/bookings");
    }

    @Test
    void shouldMapEquipmentReservationToServiceUnavailableWithSafeMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/bookings/42/confirm");

        ResponseEntity<ErrorResponse> response = handler.handleEquipmentReservation(
                new EquipmentReservationException("Inventory system timeout host=internal"),
                request);

        assertErrorResponse(
                response,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Equipment reservation is temporarily unavailable",
                "/api/v1/bookings/42/confirm");
        assertThat(response.getBody().message()).doesNotContain("Inventory system timeout");
    }

    @Test
    void shouldReturnSafeGenericErrorResponseForUnhandledException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");
        request.addHeader(ErrorResponseBuilder.REQUEST_ID_HEADER, "req-123");

        ResponseEntity<ErrorResponse> response = handler.handleException(
                new RuntimeException("internal persistence detail"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().message()).doesNotContain("internal persistence detail");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/bookings");
        assertThat(response.getBody().requestId()).isEqualTo("req-123");
    }

    private static void assertErrorResponse(
            ResponseEntity<ErrorResponse> response,
            HttpStatus status,
            String message,
            String path
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().error()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(message);
        assertThat(response.getBody().path()).isEqualTo(path);
    }
}
