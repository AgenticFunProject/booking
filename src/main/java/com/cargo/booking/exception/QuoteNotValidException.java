package com.cargo.booking.exception;

public class QuoteNotValidException extends RuntimeException {

    public QuoteNotValidException(String message) {
        super(message);
    }

    public QuoteNotValidException(String message, Throwable cause) {
        super(message, cause);
    }
}
