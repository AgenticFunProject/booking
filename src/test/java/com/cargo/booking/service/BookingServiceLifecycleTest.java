package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceLifecycleTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingReferenceGenerator bookingReferenceGenerator;

    @Mock
    private ScheduleClient scheduleClient;

    @Mock
    private EquipmentClient equipmentClient;

    @Mock
    private QuoteClient quoteClient;

    @Mock
    private BookingStateMachine bookingStateMachine;

    @Test
    void shouldStartConfirmedBooking() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.startBooking(42L);

        assertThat(result).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
        verify(bookingStateMachine).validateTransition(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldThrowWhenStartingMissingBooking() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.startBooking(404L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("404");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotStartBookingWhenTransitionIsInvalid() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateTransitionException("Invalid booking state transition from PENDING to IN_PROGRESS"))
                .when(bookingStateMachine)
                .validateTransition(BookingStatus.PENDING, BookingStatus.IN_PROGRESS);

        assertThatThrownBy(() -> bookingService.startBooking(42L))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldCompleteInProgressBooking() {
        Booking booking = bookingWithStatus(BookingStatus.IN_PROGRESS);
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.completeBooking(42L);

        assertThat(result).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingStateMachine).validateTransition(BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldThrowWhenCompletingMissingBooking() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.completeBooking(404L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("404");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotCompleteBookingWhenTransitionIsInvalid() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateTransitionException("Invalid booking state transition from CONFIRMED to COMPLETED"))
                .when(bookingStateMachine)
                .validateTransition(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

        assertThatThrownBy(() -> bookingService.completeBooking(42L))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any());
    }

    private BookingService bookingService() {
        return new BookingService(
                bookingRepository,
                bookingReferenceGenerator,
                scheduleClient,
                equipmentClient,
                quoteClient,
                bookingStateMachine
        );
    }

    private Booking bookingWithStatus(BookingStatus status) {
        return Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .status(status)
                .build();
    }
}
