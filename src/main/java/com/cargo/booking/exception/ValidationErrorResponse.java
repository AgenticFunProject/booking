package com.cargo.booking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) String requestId,
        List<FieldViolation> violations) {

    public ValidationErrorResponse {
        violations = List.copyOf(violations);
    }
}
