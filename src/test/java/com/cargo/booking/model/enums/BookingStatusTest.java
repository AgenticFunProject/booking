package com.cargo.booking.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingStatusTest {

    private static final Map<BookingStatus, Set<BookingStatus>> LIFECYCLE_TRANSITIONS =
            new EnumMap<>(BookingStatus.class);

    static {
        LIFECYCLE_TRANSITIONS.put(
                BookingStatus.PENDING,
                EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED)
        );
        LIFECYCLE_TRANSITIONS.put(
                BookingStatus.CONFIRMED,
                EnumSet.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED)
        );
        LIFECYCLE_TRANSITIONS.put(BookingStatus.IN_PROGRESS, EnumSet.of(BookingStatus.COMPLETED));
        LIFECYCLE_TRANSITIONS.put(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class));
        LIFECYCLE_TRANSITIONS.put(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    @Test
    void shouldDefineLifecycleStatusesInSpecificationOrder() {
        assertThat(BookingStatus.values())
                .containsExactly(
                        BookingStatus.PENDING,
                        BookingStatus.CONFIRMED,
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.COMPLETED,
                        BookingStatus.CANCELLED
                );
    }

    @Test
    void shouldDocumentAllowedLifecycleTargetsForEachStatus() {
        assertThat(LIFECYCLE_TRANSITIONS)
                .containsEntry(BookingStatus.PENDING, EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED))
                .containsEntry(BookingStatus.CONFIRMED, EnumSet.of(
                        BookingStatus.IN_PROGRESS,
                        BookingStatus.CANCELLED
                ))
                .containsEntry(BookingStatus.IN_PROGRESS, EnumSet.of(BookingStatus.COMPLETED))
                .containsEntry(BookingStatus.COMPLETED, EnumSet.noneOf(BookingStatus.class))
                .containsEntry(BookingStatus.CANCELLED, EnumSet.noneOf(BookingStatus.class));
    }

    @Test
    void shouldTreatCompletedAndCancelledAsTerminalStatuses() {
        assertThat(LIFECYCLE_TRANSITIONS.get(BookingStatus.COMPLETED)).isEmpty();
        assertThat(LIFECYCLE_TRANSITIONS.get(BookingStatus.CANCELLED)).isEmpty();
    }
}
