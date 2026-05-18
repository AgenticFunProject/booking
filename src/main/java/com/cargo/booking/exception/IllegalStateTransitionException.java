package com.cargo.booking.exception;

public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }

    public IllegalStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
