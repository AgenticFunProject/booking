package com.cargo.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String EQUIPMENT_RESERVATION_UNAVAILABLE_MESSAGE =
            "Equipment reservation is temporarily unavailable";
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(
            BookingNotFoundException exception,
            HttpServletRequest request
    ) {
        return warnResponse(HttpStatus.NOT_FOUND, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ErrorResponse> handleBookingValidation(
            BookingValidationException exception,
            HttpServletRequest request
    ) {
        return warnResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateTransition(
            IllegalStateTransitionException exception,
            HttpServletRequest request
    ) {
        return warnResponse(HttpStatus.CONFLICT, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(ScheduleNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleScheduleNotAvailable(
            ScheduleNotAvailableException exception,
            HttpServletRequest request
    ) {
        return warnResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(QuoteNotValidException.class)
    public ResponseEntity<ErrorResponse> handleQuoteNotValid(
            QuoteNotValidException exception,
            HttpServletRequest request
    ) {
        return warnResponse(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception, request);
    }

    @ExceptionHandler(EquipmentReservationException.class)
    public ResponseEntity<ErrorResponse> handleEquipmentReservation(
            EquipmentReservationException exception,
            HttpServletRequest request
    ) {
        logError(exception, request);

        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, EQUIPMENT_RESERVATION_UNAVAILABLE_MESSAGE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        logError(exception, request);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ErrorResponse> warnResponse(
            HttpStatus status,
            String message,
            Exception exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Request failed class={} message={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER));

        return buildResponse(status, message, request);
    }

    private void logError(Exception exception, HttpServletRequest request) {
        log.error(
                "Request failed class={} message={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER),
                exception);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponseBuilder.buildError(status, message, request));
    }
}
