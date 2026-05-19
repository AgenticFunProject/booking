package com.cargo.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public final class ErrorResponseBuilder {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private ErrorResponseBuilder() {
    }

    public static ErrorResponse buildError(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                requestId(request));
    }

    public static ValidationErrorResponse buildValidationError(
            HttpStatus status,
            String message,
            List<FieldViolation> violations,
            HttpServletRequest request) {
        return new ValidationErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                requestId(request),
                violations);
    }

    private static String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId;
    }
}
