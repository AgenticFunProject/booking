package com.cargo.booking.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

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
}
