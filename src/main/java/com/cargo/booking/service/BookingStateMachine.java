package com.cargo.booking.service;

import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.enums.BookingStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(BookingStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BookingStatus.PENDING, EnumSet.of(
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED
        ));
        ALLOWED_TRANSITIONS.put(BookingStatus.CONFIRMED, EnumSet.of(
                BookingStatus.IN_PROGRESS,
                BookingStatus.CANCELLED
        ));
        ALLOWED_TRANSITIONS.put(BookingStatus.IN_PROGRESS, EnumSet.of(BookingStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        ALLOWED_TRANSITIONS.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    public void validateTransition(BookingStatus current, BookingStatus target) {
        if (current == null || target == null || !ALLOWED_TRANSITIONS
                .getOrDefault(current, Set.of())
                .contains(target)) {
            throw new IllegalStateTransitionException(
                    "Invalid booking state transition from %s to %s".formatted(current, target)
            );
        }
    }
}
