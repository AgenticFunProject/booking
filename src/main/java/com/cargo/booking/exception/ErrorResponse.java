package com.cargo.booking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) String requestId) {
}
