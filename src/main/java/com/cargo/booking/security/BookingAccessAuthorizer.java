package com.cargo.booking.security;

import org.springframework.stereotype.Component;

@Component
public class BookingAccessAuthorizer {

    public void authorizeBookingAccess(Long bookingId) {
        // Ownership checks are wired here by the security implementation phase.
    }
}
