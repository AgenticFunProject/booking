package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.client.dto.EquipmentLineDTO;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.exception.EquipmentReservationException;
import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.repository.BookingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceConfirmTest {

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
    void shouldConfirmPendingBookingAndReserveEquipment() {
        Booking booking = pendingBooking();
        BookingService bookingService = bookingService();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EquipmentLineDTO>> equipmentCaptor = ArgumentCaptor.forClass(List.class);

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking confirmedBooking = bookingService.confirmBooking(42L);

        verify(bookingStateMachine).validateTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED);
        verify(equipmentClient).reserveEquipment(eq(42L), equipmentCaptor.capture());
        verify(bookingRepository).save(booking);

        assertThat(confirmedBooking).isSameAs(booking);
        assertThat(confirmedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(equipmentCaptor.getValue())
                .containsExactly(
                        new EquipmentLineDTO("20FT", 2),
                        new EquipmentLineDTO("REEFER", 1)
                );
    }

    @Test
    void shouldThrowWhenBookingIsMissing() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmBooking(404L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("404");

        verify(bookingStateMachine, never()).validateTransition(any(), any());
        verify(equipmentClient, never()).reserveEquipment(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidTransitionBeforeReservingEquipment() {
        Booking booking = pendingBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        doThrow(new IllegalStateTransitionException("Invalid booking state transition from COMPLETED to CONFIRMED"))
                .when(bookingStateMachine)
                .validateTransition(BookingStatus.COMPLETED, BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> bookingService.confirmBooking(42L))
                .isInstanceOf(IllegalStateTransitionException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(equipmentClient, never()).reserveEquipment(any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotChangeStatusWhenEquipmentReservationFails() {
        Booking booking = pendingBooking();
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));
        doThrow(new EquipmentReservationException("Equipment unavailable"))
                .when(equipmentClient)
                .reserveEquipment(any(), any());

        assertThatThrownBy(() -> bookingService.confirmBooking(42L))
                .isInstanceOf(EquipmentReservationException.class)
                .hasMessage("Equipment unavailable");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
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

    private Booking pendingBooking() {
        Booking booking = Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .status(BookingStatus.PENDING)
                .build();

        BookingEquipmentLine dryContainer = BookingEquipmentLine.builder()
                .type(EquipmentType.TWENTY_FT)
                .quantity(2)
                .booking(booking)
                .build();
        BookingEquipmentLine reefer = BookingEquipmentLine.builder()
                .type(EquipmentType.REEFER)
                .quantity(1)
                .booking(booking)
                .build();

        booking.getEquipmentLines().addAll(List.of(dryContainer, reefer));
        return booking;
    }
}
