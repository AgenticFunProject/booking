package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.exception.EquipmentReservationException;
import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
import com.cargo.booking.testutil.TestDataBuilder;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceCancelTest {

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
    void shouldCancelPendingBookingWithoutReleasingEquipment() {
        Booking booking = bookingWithStatus(BookingStatus.PENDING);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.cancelBooking(42L);

        assertThat(result).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingStateMachine).validateTransition(BookingStatus.PENDING, BookingStatus.CANCELLED);
        verify(equipmentClient, never()).releaseEquipment(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldCancelConfirmedBookingAndReleaseEquipment() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking result = bookingService.cancelBooking(42L);

        assertThat(result).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        InOrder cancelOrder = inOrder(bookingStateMachine, equipmentClient, bookingRepository);
        cancelOrder.verify(bookingStateMachine).validateTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);
        cancelOrder.verify(equipmentClient).releaseEquipment(42L);
        cancelOrder.verify(bookingRepository).save(booking);
    }

    @Test
    void shouldCancelConfirmedBookingWhenEquipmentReleaseFails() {
        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        doThrow(new EquipmentReservationException("Release unavailable"))
                .when(equipmentClient)
                .releaseEquipment(42L);

        Booking result = bookingService.cancelBooking(42L);

        assertThat(result).isSameAs(booking);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(equipmentClient).releaseEquipment(42L);
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldThrowWhenCancellingMissingBooking() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(404L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("404");

        verify(bookingStateMachine, never()).validateTransition(any(), any());
        verify(equipmentClient, never()).releaseEquipment(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidCancelTransitionBeforeReleasingEquipment() {
        Booking booking = bookingWithStatus(BookingStatus.IN_PROGRESS);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateTransitionException("Invalid booking state transition from IN_PROGRESS to CANCELLED"))
                .when(bookingStateMachine)
                .validateTransition(BookingStatus.IN_PROGRESS, BookingStatus.CANCELLED);

        assertThatThrownBy(() -> bookingService.cancelBooking(42L))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
        verify(equipmentClient, never()).releaseEquipment(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCancellingCompletedBookingWithoutChangingStatus() {
        Booking booking = bookingWithStatus(BookingStatus.COMPLETED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateTransitionException("Invalid booking state transition from COMPLETED to CANCELLED"))
                .when(bookingStateMachine)
                .validateTransition(BookingStatus.COMPLETED, BookingStatus.CANCELLED);

        assertThatThrownBy(() -> bookingService.cancelBooking(42L))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(equipmentClient, never()).releaseEquipment(any());
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
                .id(TestDataBuilder.DEFAULT_BOOKING_ID)
                .bookingReference(TestDataBuilder.DEFAULT_BOOKING_REFERENCE)
                .status(status)
                .build();
    }
}
