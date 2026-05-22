package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
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
import com.cargo.booking.testutil.TestDataBuilder;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

        when(scheduleClient.validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID)).thenReturn(true);
        when(quoteClient.validateQuote(
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG
        )).thenReturn(true);
        when(bookingReferenceGenerator.generateReference()).thenReturn("BKG-2026-00042");
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(request);

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();

        assertThat(booking).isSameAs(savedBooking);
        assertThat(savedBooking.getBookingReference()).isEqualTo("BKG-2026-00042");
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getCustomerId()).isEqualTo(TestDataBuilder.DEFAULT_CUSTOMER_ID);
        assertThat(savedBooking.getEquipmentLines())
                .extracting(BookingEquipmentLine::getType)
                .containsExactly(EquipmentType.TWENTY_FT, EquipmentType.REEFER);
        assertThat(savedBooking.getEquipmentLines())
                .allSatisfy(line -> assertThat(line.getBooking()).isSameAs(savedBooking));

        InOrder createOrder = inOrder(scheduleClient, quoteClient, bookingReferenceGenerator, bookingRepository);
        createOrder.verify(scheduleClient).validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID);
        createOrder.verify(quoteClient).validateQuote(
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG
        );
        createOrder.verify(bookingReferenceGenerator).generateReference();
        createOrder.verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void shouldThrowWhenRequestIsMissing() {
        BookingService bookingService = bookingService();

        assertThatThrownBy(() -> bookingService.createBooking(null))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Booking request is required");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEquipmentListIsEmpty() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
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
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(TestDataBuilder.aServiceEquipmentLineRequest())
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Customer id is required");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenScheduleIdIsMissing() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                null,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(TestDataBuilder.aServiceEquipmentLineRequest())
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Schedule id is required");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenQuoteIdIsMissing() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                null,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(TestDataBuilder.aServiceEquipmentLineRequest())
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Quote id is required");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCustomerNameIsBlank() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                " ",
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(TestDataBuilder.aServiceEquipmentLineRequest())
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Customer name is required");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCargoWeightIsNotPositive() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                BigDecimal.ZERO,
                List.of(TestDataBuilder.aServiceEquipmentLineRequest())
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Cargo weight must be greater than zero");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEquipmentTypeIsUnsupported() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(new CreateBookingRequest.EquipmentLineRequest("TANK", 1))
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessageContaining("Unsupported equipment type: TANK");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEquipmentLineIsMissing() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                Collections.singletonList(null)
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Equipment line must not be null");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEquipmentQuantityIsInvalid() {
        BookingService bookingService = bookingService();
        CreateBookingRequest request = new CreateBookingRequest(
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(new CreateBookingRequest.EquipmentLineRequest("20FT", 0))
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingValidationException.class)
                .hasMessage("Equipment quantity must be greater than zero");

        verify(scheduleClient, never()).validateSchedule(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenScheduleIsUnavailable() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(ScheduleNotAvailableException.class)
                .hasMessageContaining(TestDataBuilder.DEFAULT_SCHEDULE_ID.toString());

        verify(quoteClient, never()).validateQuote(any(), any(), any());
        verify(bookingReferenceGenerator, never()).generateReference();
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldNotPersistBookingWhenScheduleClientThrows() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID))
                .thenThrow(new ScheduleNotAvailableException("Schedule API unavailable"));

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(ScheduleNotAvailableException.class)
                .hasMessage("Schedule API unavailable");

        verify(quoteClient, never()).validateQuote(any(), any(), any());
        verify(bookingReferenceGenerator, never()).generateReference();
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenQuoteIsInvalid() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID)).thenReturn(true);
        when(quoteClient.validateQuote(
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG
        )).thenReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(QuoteNotValidException.class)
                .hasMessageContaining(TestDataBuilder.DEFAULT_QUOTE_ID.toString())
                .hasMessageContaining(TestDataBuilder.DEFAULT_SCHEDULE_ID.toString());

        verify(bookingRepository, never()).save(any());
        verify(bookingReferenceGenerator, never()).generateReference();
    }

    @Test
    void shouldNotPersistBookingWhenQuoteClientThrows() {
        BookingService bookingService = bookingService();

        when(scheduleClient.validateSchedule(TestDataBuilder.DEFAULT_SCHEDULE_ID)).thenReturn(true);
        when(quoteClient.validateQuote(
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG
        )).thenThrow(new QuoteNotValidException("Quote API unavailable"));

        assertThatThrownBy(() -> bookingService.createBooking(validRequest()))
                .isInstanceOf(QuoteNotValidException.class)
                .hasMessage("Quote API unavailable");

        verify(bookingReferenceGenerator, never()).generateReference();
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
                TestDataBuilder.DEFAULT_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                TestDataBuilder.DEFAULT_CUSTOMER_PHONE,
                TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG,
                List.of(
                        new CreateBookingRequest.EquipmentLineRequest("20FT", 2),
                        new CreateBookingRequest.EquipmentLineRequest("REEFER", 1)
                )
        );
    }

}
