package com.cargo.booking.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cargo.booking.model.enums.BookingStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapBusinessExceptionsToConfiguredStatuses() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings/42");

        assertError(handler.handleBookingNotFound(new BookingNotFoundException("not found"), request),
                HttpStatus.NOT_FOUND, "not found");
        assertError(handler.handleIllegalStateTransition(new IllegalStateTransitionException("bad transition"), request),
                HttpStatus.CONFLICT, "bad transition");
        assertError(handler.handleUnprocessableBusinessException(new ScheduleNotAvailableException("schedule closed"), request),
                HttpStatus.UNPROCESSABLE_ENTITY, "schedule closed");
        assertError(handler.handleUnprocessableBusinessException(new QuoteNotValidException("quote expired"), request),
                HttpStatus.UNPROCESSABLE_ENTITY, "quote expired");
        assertError(handler.handleBookingValidation(new BookingValidationException("invalid id"), request),
                HttpStatus.BAD_REQUEST, "invalid id");
    }

    @Test
    void shouldReturnSafeEquipmentReservationMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/bookings/42/confirm");

        ResponseEntity<ErrorResponse> response = handler.handleEquipmentReservation(
                new EquipmentReservationException("provider host timeout"),
                request);

        assertError(response, HttpStatus.SERVICE_UNAVAILABLE, "Equipment reservation is temporarily unavailable");
        assertThat(response.getBody().message()).doesNotContain("provider host timeout");
    }

    @Test
    void shouldMapMethodArgumentNotValidToSortedValidationResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");
        request.addHeader(ErrorResponseBuilder.REQUEST_ID_HEADER, "req-123");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request",
                "customer.email",
                "not-an-email",
                false,
                null,
                null,
                "must be a well-formed email address"));
        bindingResult.addError(new FieldError(
                "request",
                "cargo.weightKg",
                -5,
                false,
                null,
                null,
                "must be greater than or equal to 0.01"));

        ResponseEntity<ValidationErrorResponse> response = handler.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(methodParameter(), bindingResult),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed with 2 error(s)");
        assertThat(response.getBody().requestId()).isEqualTo("req-123");
        assertThat(response.getBody().violations())
                .extracting(FieldViolation::field)
                .containsExactly("cargo.weightKg", "customer.email");
        assertThat(response.getBody().violations().getFirst().rejectedValue()).isEqualTo(-5);
    }

    @Test
    void shouldMapConstraintViolationsToValidationResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");

        ResponseEntity<ValidationErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation("status", "must not be null", null))),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed with 1 error(s)");
        assertThat(response.getBody().violations()).containsExactly(new FieldViolation("status", "must not be null", null));
    }

    @Test
    void shouldMapUnreadableJsonWithoutExposingParserDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");

        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("low-level parser detail"),
                request);

        assertError(response, HttpStatus.BAD_REQUEST, "Malformed JSON request body");
        assertThat(response.getBody().message()).doesNotContain("low-level parser detail");
    }

    @Test
    void shouldMapMissingParameterAndTypeMismatch() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bookings");

        assertError(handler.handleMissingServletRequestParameter(
                        new MissingServletRequestParameterException("customerId", "Long"),
                        request),
                HttpStatus.BAD_REQUEST,
                "Required parameter 'customerId' is missing");
        assertError(handler.handleMethodArgumentTypeMismatch(
                        new MethodArgumentTypeMismatchException(
                                "bad-status",
                                BookingStatus.class,
                                "status",
                                null,
                                null),
                        request),
                HttpStatus.BAD_REQUEST,
                "Parameter 'status' must be a valid BookingStatus");
    }

    @Test
    void shouldMapHttpMethodMediaAndNoHandlerErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/bookings/42");

        assertError(handler.handleHttpRequestMethodNotSupported(
                        new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST", "PATCH")),
                        request),
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method 'DELETE' is not supported. Supported methods: GET, POST, PATCH");
        assertError(handler.handleHttpMediaTypeNotSupported(
                        new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)),
                        request),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type 'text/plain' is not supported. Supported media types: application/json");
        assertError(handler.handleNoHandlerFound(
                        new NoHandlerFoundException("DELETE", "/api/v1/bookings/42", new HttpHeaders()),
                        request),
                HttpStatus.NOT_FOUND,
                "No endpoint found for DELETE /api/v1/bookings/42");
    }

    @Test
    void shouldMapAccessDeniedFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/bookings/42/cancel");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("customer mismatch"),
                request);

        assertError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
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

    private static void assertError(ResponseEntity<ErrorResponse> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().error()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo(message);
    }

    private static MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validatedRequest", Object.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void validatedRequest(Object request) {
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<?> violation(String field, String message, Object invalidValue) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(field);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        when(violation.getInvalidValue()).thenReturn(invalidValue);
        return violation;
    }
}
