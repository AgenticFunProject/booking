package com.cargo.booking.exception;

public record FieldViolation(String field, String message, Object rejectedValue) {
}
