package com.cargo.booking.exception;

public class ScheduleNotAvailableException extends RuntimeException {

    public ScheduleNotAvailableException(String message) {
        super(message);
    }

    public ScheduleNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
