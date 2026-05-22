package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.config.RequestTracingMdc;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BookingServiceReadTest {

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

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldGetBookingByIdWithEquipmentLines() {
        Booking booking = Booking.builder().id(42L).bookingReference("BKG-2026-00042").build();
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(42L)).thenReturn(Optional.of(booking));

        assertThat(bookingService.getBookingById(42L)).isSameAs(booking);
    }

    @Test
    void shouldThrowWhenBookingIdIsMissing() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(404L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void shouldGetBookingByReferenceWithEquipmentLines() {
        Booking booking = Booking.builder().id(42L).bookingReference("BKG-2026-00042").build();
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesByBookingReference("BKG-2026-00042"))
                .thenAnswer(invocation -> {
                    assertThat(MDC.get(RequestTracingMdc.BOOKING_REF)).isEqualTo("BKG-2026-00042");
                    return Optional.of(booking);
                });

        assertThat(bookingService.getBookingByReference("BKG-2026-00042")).isSameAs(booking);
        assertThat(MDC.get(RequestTracingMdc.BOOKING_REF)).isNull();
    }

    @Test
    void shouldThrowWhenBookingReferenceIsMissing() {
        BookingService bookingService = bookingService();

        when(bookingRepository.findWithEquipmentLinesByBookingReference("BKG-2026-40404"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingByReference("BKG-2026-40404"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("BKG-2026-40404");
    }

    @Test
    void shouldListBookingsByCustomerAndStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> bookings = new PageImpl<>(List.of(Booking.builder().id(1L).build()));
        BookingService bookingService = bookingService();

        when(bookingRepository.findByCustomerIdAndStatus(7L, BookingStatus.PENDING, pageable))
                .thenReturn(bookings);

        assertThat(bookingService.getBookings(7L, BookingStatus.PENDING, pageable)).isSameAs(bookings);
    }

    @Test
    void shouldListBookingsByCustomerOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> bookings = new PageImpl<>(List.of(Booking.builder().id(1L).build()));
        BookingService bookingService = bookingService();

        when(bookingRepository.findByCustomerId(7L, pageable)).thenReturn(bookings);

        assertThat(bookingService.getBookings(7L, null, pageable)).isSameAs(bookings);
        verify(bookingRepository).findByCustomerId(7L, pageable);
    }

    @Test
    void shouldListBookingsByStatusOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> bookings = new PageImpl<>(List.of(Booking.builder().id(1L).build()));
        BookingService bookingService = bookingService();

        when(bookingRepository.findByStatus(BookingStatus.CONFIRMED, pageable)).thenReturn(bookings);

        assertThat(bookingService.getBookings(null, BookingStatus.CONFIRMED, pageable)).isSameAs(bookings);
        verify(bookingRepository).findByStatus(BookingStatus.CONFIRMED, pageable);
    }

    @Test
    void shouldListAllBookingsWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Booking> bookings = new PageImpl<>(List.of(Booking.builder().id(1L).build()));
        BookingService bookingService = bookingService();

        when(bookingRepository.findAll(pageable)).thenReturn(bookings);

        assertThat(bookingService.getBookings(null, null, pageable)).isSameAs(bookings);
        verify(bookingRepository).findAll(pageable);
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
}
