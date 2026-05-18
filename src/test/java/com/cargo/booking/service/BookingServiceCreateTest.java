package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.exception.QuoteNotValidException;
import com.cargo.booking.exception.ScheduleNotAvailableException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceCreateTest {

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
    void shouldCreateBookingWithPendingStatusAndEquipmentLines() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = validRequest();
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);

        when(scheduleClient.validateSchedule(10L)).thenReturn(true);
        when(quoteClient.validateQuote(20L, 10L, new BigDecimal("1200.50"))).thenReturn(true);
        when(bookingReferenceGenerator.generateReference()).thenReturn("BKG-2026-00042");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(request);

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();

        assertThat(booking).isSameAs(savedBooking);
        assertThat(savedBooking.getBookingReference()).isEqualTo("BKG-2026-00042");
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getCustomerId()).isEqualTo(7L);
        assertThat(savedBooking.getEquipmentLines())
                .extracting(BookingEquipmentLine::getType)
                .containsExactly(EquipmentType.TWENTY_FT, EquipmentType.REEFER);
        assertThat(savedBooking.getEquipmentLines())
                .allSatisfy(line -> assertThat(line.getBooking()).isSameAs(savedBooking));
    }

    @Test
    void shouldThrowWhenEquipmentListIsEmpty() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                7L,
                10L,
                20L,
                "Acme Logistics",
                "ops@example.com",
                "555-0100",
                "Machine parts",
                new BigDecimal("1200.50"),
                List.of()
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("At least one equipment line is required");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCustomerIdIsMissing() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                null,
                10L,
                20L,
                "Acme Logistics",
                "ops@example.com",
                "555-0100",
                "Machine parts",
                new BigDecimal("1200.50"),
                List.of(new CreateBookingRequest.EquipmentLineRequest("20FT", 2))
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Customer id is required");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEquipmentTypeIsUnsupported() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                7L,
                10L,
                20L,
                "Acme Logistics",
                "ops@example.com",
                "555-0100",
                "Machine parts",
                new BigDecimal("1200.50"),
                List.of(new CreateBookingRequest.EquipmentLineRequest("TANK", 1))
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Unsupported equipment type: TANK");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenScheduleIsUnavailable() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(10L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(ScheduleNotAvailableException.class)
                .hasMessageContaining("10");

        verify(quoteClient, never()).validateQuote(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenQuoteIsInvalid() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(10L)).thenReturn(true);
        when(quoteClient.validateQuote(20L, 10L, new BigDecimal("1200.50"))).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(QuoteNotValidException.class)
                .hasMessageContaining("20")
                .hasMessageContaining("10");

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

    private CreateBookingRequest validRequest() {
        return new CreateBookingRequest(
                7L,
                10L,
                20L,
                "Acme Logistics",
                "ops@example.com",
                "555-0100",
                "Machine parts",
                new BigDecimal("1200.50"),
                List.of(
                        new CreateBookingRequest.EquipmentLineRequest("20FT", 2),
                        new CreateBookingRequest.EquipmentLineRequest("REEFER", 1)
                )
        );
    }
}
