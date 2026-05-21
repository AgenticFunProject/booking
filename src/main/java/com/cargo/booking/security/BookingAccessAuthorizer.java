package com.cargo.booking.security;

import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.repository.BookingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingAccessAuthorizer {

    private static final String ACCESS_DENIED_MESSAGE = "Customer ownership check failed";
    private static final String MISSING_CUSTOMER_CLAIM_MESSAGE = "Customer identity claim is required";
    private static final String MISSING_LIST_CUSTOMER_MESSAGE =
            "customerId query parameter is required for customer callers";

    private final BookingRepository bookingRepository;
    private final SecurityProperties securityProperties;

    public void authorizeCreateCustomer(Long customerId) {
        authorizeCustomerId(customerId);
    }

    public void authorizeListCustomer(Long customerId) {
        if (shouldSkipOwnershipCheck()) {
            return;
        }

        Long tokenCustomerId = requireCurrentCustomerId();
        if (customerId == null) {
            throw new BookingValidationException(MISSING_LIST_CUSTOMER_MESSAGE);
        }
        requireMatchingCustomer(customerId, tokenCustomerId);
    }

    public void authorizeBookingAccess(Long bookingId) {
        if (shouldSkipOwnershipCheck()) {
            return;
        }

        Long tokenCustomerId = requireCurrentCustomerId();
        bookingRepository.findById(bookingId)
                .map(Booking::getCustomerId)
                .ifPresent(ownerId -> requireMatchingCustomer(ownerId, tokenCustomerId));
    }

    public void authorizeBookingAccess(String reference) {
        if (shouldSkipOwnershipCheck()) {
            return;
        }

        Long tokenCustomerId = requireCurrentCustomerId();
        bookingRepository.findByBookingReference(reference)
                .map(Booking::getCustomerId)
                .ifPresent(ownerId -> requireMatchingCustomer(ownerId, tokenCustomerId));
    }

    private void authorizeCustomerId(Long customerId) {
        if (shouldSkipOwnershipCheck()) {
            return;
        }

        requireMatchingCustomer(customerId, requireCurrentCustomerId());
    }

    private boolean shouldSkipOwnershipCheck() {
        return !securityProperties.enabled()
                || SecurityContextHelper.hasRole("SERVICE")
                || SecurityContextHelper.hasRole("OPERATOR")
                || SecurityContextHelper.hasRole("ADMIN");
    }

    private Long requireCurrentCustomerId() {
        Optional<Long> customerId = SecurityContextHelper.getCurrentCustomerId();
        if (customerId.isEmpty()) {
            throw new AccessDeniedException(MISSING_CUSTOMER_CLAIM_MESSAGE);
        }
        return customerId.get();
    }

    private void requireMatchingCustomer(Long checkedCustomerId, Long tokenCustomerId) {
        if (!tokenCustomerId.equals(checkedCustomerId)) {
            throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
        }
    }
}
