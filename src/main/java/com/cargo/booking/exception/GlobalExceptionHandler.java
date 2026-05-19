package com.cargo.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";
    private static final String ACCESS_DENIED_MESSAGE = "You do not have permission to perform this action";

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(
            BookingNotFoundException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateTransition(
            IllegalStateTransitionException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({
            ScheduleNotAvailableException.class,
            QuoteNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleUnprocessableBusinessException(
            RuntimeException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(EquipmentReservationException.class)
    public ResponseEntity<ErrorResponse> handleEquipmentReservation(
            EquipmentReservationException exception,
            HttpServletRequest request) {
        String message = "Equipment reservation is temporarily unavailable";
        error(exception, HttpStatus.SERVICE_UNAVAILABLE, message, request);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponseBuilder.buildError(HttpStatus.SERVICE_UNAVAILABLE, message, request));
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ErrorResponse> handleBookingValidation(
            BookingValidationException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        String message = "Validation failed with " + violations.size() + " error(s)";

        warnValidation(exception, violations.size(), request);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseBuilder.buildValidationError(
                        HttpStatus.BAD_REQUEST,
                        message,
                        violations,
                        request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .sorted(Comparator.comparing(FieldViolation::field))
                .toList();
        String message = "Validation failed with " + violations.size() + " error(s)";

        warnValidation(exception, violations.size(), request);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseBuilder.buildValidationError(
                        HttpStatus.BAD_REQUEST,
                        message,
                        violations,
                        request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        String message = "Required parameter '" + exception.getParameterName() + "' is missing";
        return warn(exception, HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        String expectedType = exception.getRequiredType() == null
                ? "value"
                : exception.getRequiredType().getSimpleName();
        String message = "Parameter '" + exception.getName() + "' must be a valid " + expectedType;
        return warn(exception, HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        String supportedMethods = exception.getSupportedMethods() == null
                ? "none"
                : String.join(", ", exception.getSupportedMethods());
        String message = "Method '" + exception.getMethod() + "' is not supported. Supported methods: "
                + supportedMethods;
        return warn(exception, HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        String supportedMediaTypes = exception.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        if (supportedMediaTypes.isBlank()) {
            supportedMediaTypes = "none";
        }
        String message = "Content type '" + exception.getContentType()
                + "' is not supported. Supported media types: " + supportedMediaTypes;
        return warn(exception, HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request) {
        String message = "No endpoint found for " + request.getMethod() + " " + request.getRequestURI();
        return warn(exception, HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return warn(exception, HttpStatus.FORBIDDEN, ACCESS_DENIED_MESSAGE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        error(exception, HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, request);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseBuilder.buildError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        UNEXPECTED_ERROR_MESSAGE,
                        request));
    }

    private FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    private FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        return new FieldViolation(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue());
    }

    private ResponseEntity<ErrorResponse> warn(
            Exception exception,
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        log.warn(
                "Handled exception class={} message={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER));

        return ResponseEntity
                .status(status)
                .body(ErrorResponseBuilder.buildError(status, message, request));
    }

    private void warnValidation(Exception exception, int violationCount, HttpServletRequest request) {
        log.warn(
                "Handled validation exception class={} message={} violations={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                violationCount,
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER));
    }

    private void error(Exception exception, HttpStatus status, String responseMessage, HttpServletRequest request) {
        log.error(
                "Handled exception class={} message={} responseStatus={} responseMessage={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                status.value(),
                responseMessage,
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER),
                exception);
    }
}
