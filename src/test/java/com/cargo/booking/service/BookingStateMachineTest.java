package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.enums.BookingStatus;
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
    void shouldRejectNullStatuses() {
        assertThatThrownBy(() -> stateMachine.validateTransition(null, BookingStatus.CONFIRMED))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> stateMachine.validateTransition(BookingStatus.PENDING, null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }
}
