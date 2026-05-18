package com.cargo.booking.exception;

public class EquipmentReservationException extends RuntimeException {

    public EquipmentReservationException(String message) {
        super(message);
    }

    public EquipmentReservationException(String message, Throwable cause) {
        super(message, cause);
    }
}
