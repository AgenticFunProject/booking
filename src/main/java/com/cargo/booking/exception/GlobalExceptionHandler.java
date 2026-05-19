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
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error(
                "Unhandled exception class={} message={} method={} path={} requestId={}",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader(ErrorResponseBuilder.REQUEST_ID_HEADER),
                exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseBuilder.buildError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        UNEXPECTED_ERROR_MESSAGE,
                        request));
    }
}
