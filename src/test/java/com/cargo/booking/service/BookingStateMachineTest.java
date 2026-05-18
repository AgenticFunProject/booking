package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.enums.BookingStatus;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BookingStateMachineTest {

    private final BookingStateMachine stateMachine = new BookingStateMachine();

    @Test
    void shouldAllowValidTransitions() {
        assertThatCode(() -> stateMachine.validateTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(BookingStatus.PENDING, BookingStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED))
                .doesNotThrowAnyException();
        assertThatCode(() -> stateMachine.validateTransition(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidTransitionAndIncludeStatusesInMessage() {
        assertThatThrownBy(() -> stateMachine.validateTransition(BookingStatus.COMPLETED, BookingStatus.CANCELLED))
                .isInstanceOf(IllegalStateTransitionException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void shouldRejectEveryTransitionNotAllowedByLifecycleRules() {
        for (BookingStatus current : BookingStatus.values()) {
            for (BookingStatus target : BookingStatus.values()) {
                if (allowedTargets(current).contains(target)) {
                    continue;
                }

                assertThatThrownBy(() -> stateMachine.validateTransition(current, target))
                        .as("transition from %s to %s", current, target)
                        .isInstanceOf(IllegalStateTransitionException.class)
                        .hasMessageContaining(current.name())
                        .hasMessageContaining(target.name());
            }
        }
    }

    @Test
    void shouldRejectNullStatuses() {
        assertThatThrownBy(() -> stateMachine.validateTransition(null, BookingStatus.CONFIRMED))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> stateMachine.validateTransition(BookingStatus.PENDING, null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    private Set<BookingStatus> allowedTargets(BookingStatus current) {
        return switch (current) {
            case PENDING -> EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
            case CONFIRMED -> EnumSet.of(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED);
            case IN_PROGRESS -> EnumSet.of(BookingStatus.COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(BookingStatus.class);
        };
    }
}
