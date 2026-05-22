package com.cargo.booking.config;

import com.cargo.booking.security.SecurityContextHelper;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public final class RequestTracingMdc {

    public static final String REQUEST_ID = "requestId";
    public static final String PRINCIPAL = "principal";
    public static final String CUSTOMER_ID = "customerId";
    public static final String BOOKING_REF = "bookingRef";

    private RequestTracingMdc() {
    }

    static void putRequestId(String requestId) {
        putIfText(REQUEST_ID, requestId);
    }

    public static void putAuthenticatedRequester() {
        String subject = SecurityContextHelper.getCurrentSubject();
        if (StringUtils.hasText(subject)) {
            MDC.put(PRINCIPAL, subject);
        } else {
            MDC.remove(PRINCIPAL);
        }

        SecurityContextHelper.getCurrentCustomerId()
                .ifPresentOrElse(
                        customerId -> MDC.put(CUSTOMER_ID, customerId.toString()),
                        () -> MDC.remove(CUSTOMER_ID)
                );
    }

    public static void putBookingReference(String bookingReference) {
        putIfText(BOOKING_REF, bookingReference);
    }

    public static void clearAuthentication() {
        MDC.remove(PRINCIPAL);
        MDC.remove(CUSTOMER_ID);
    }

    public static void clearBookingReference() {
        MDC.remove(BOOKING_REF);
    }

    static void clearRequest() {
        MDC.remove(REQUEST_ID);
        clearAuthentication();
        clearBookingReference();
    }

    private static void putIfText(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value.trim());
        } else {
            MDC.remove(key);
        }
    }
}
