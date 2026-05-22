package com.cargo.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cargo.booking.dto.request.CargoRequest;
import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.CustomerRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.dto.response.BookingResponse;
import com.cargo.booking.dto.response.CargoResponse;
import com.cargo.booking.dto.response.CustomerResponse;
import com.cargo.booking.dto.response.EquipmentResponse;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.exception.GlobalExceptionHandler;
import com.cargo.booking.exception.IllegalStateTransitionException;
import com.cargo.booking.exception.QuoteNotValidException;
import com.cargo.booking.exception.ScheduleNotAvailableException;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.security.BookingAccessAuthorizer;
import com.cargo.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.mockito.InOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
class BookingControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingAccessAuthorizer bookingAccessAuthorizer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(
                        bookingService,
                        bookingMapper,
                        bookingAccessAuthorizer
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(maxSizePageableResolver())
                .build();
    }

    @Test
    void shouldCreateBooking() throws Exception {
        CreateBookingRequest request = validRequest();
        Booking savedBooking = savedBooking();
        BookingCreatedResponse response = new BookingCreatedResponse(
                42L,
                "BKG-2026-00042",
                3001L,
                "PENDING",
                Instant.parse("2026-05-19T12:00:00Z")
        );
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class)))
                .thenReturn(savedBooking);
        when(bookingMapper.toCreatedResponse(savedBooking)).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-19T12:00:00Z"));

        ArgumentCaptor<com.cargo.booking.service.CreateBookingRequest> serviceRequest =
                ArgumentCaptor.forClass(com.cargo.booking.service.CreateBookingRequest.class);
        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService, bookingMapper);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeCreateCustomer(3001L);
        orderedCalls.verify(bookingService).createBooking(serviceRequest.capture());
        orderedCalls.verify(bookingMapper).toCreatedResponse(savedBooking);

        assertThat(serviceRequest.getValue().customerId()).isEqualTo(3001L);
        assertThat(serviceRequest.getValue().scheduleId()).isEqualTo(1001L);
        assertThat(serviceRequest.getValue().quoteId()).isEqualTo(2001L);
        assertThat(serviceRequest.getValue().customerName()).isEqualTo("Acme Shipping Co.");
        assertThat(serviceRequest.getValue().customerEmail()).isEqualTo("logistics@acme.com");
        assertThat(serviceRequest.getValue().customerPhone()).isEqualTo("+36-1-234-5678");
        assertThat(serviceRequest.getValue().cargoDescription()).isEqualTo("Industrial machinery parts");
        assertThat(serviceRequest.getValue().cargoWeightKg()).isEqualByComparingTo("12000.00");
        assertThat(serviceRequest.getValue().equipment())
                .extracting(
                        com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest::type,
                        com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest::quantity
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("20FT", 2),
                        org.assertj.core.groups.Tuple.tuple("40HC", 1)
                );
    }

    @Test
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed with 6 error(s)"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"))
                .andExpect(jsonPath("$.violations[*].field", containsInAnyOrder(
                        "cargo",
                        "customer",
                        "customerId",
                        "equipment",
                        "quoteId",
                        "scheduleId"
                )));

        verifyNoMoreInteractions(bookingAccessAuthorizer, bookingService, bookingMapper);
    }

    @Test
    void shouldReturn400WhenEmailInvalid() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(
                3001L,
                1001L,
                2001L,
                new CustomerRequest("Acme Shipping Co.", "not-an-email", "+36-1-234-5678"),
                new CargoRequest("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(new EquipmentRequest("20FT", 2))
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed with 1 error(s)"))
                .andExpect(jsonPath("$.violations[0].field").value("customer.email"))
                .andExpect(jsonPath("$.violations[0].rejectedValue").value("not-an-email"));

        verifyNoMoreInteractions(bookingAccessAuthorizer, bookingService, bookingMapper);
    }

    @Test
    void shouldReturn400WhenEquipmentListEmpty() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest(
                3001L,
                1001L,
                2001L,
                new CustomerRequest("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoRequest("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of()
        );

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed with 1 error(s)"))
                .andExpect(jsonPath("$.violations[0].field").value("equipment"));

        verifyNoMoreInteractions(bookingAccessAuthorizer, bookingService, bookingMapper);
    }

    @Test
    void shouldReturn400WhenEquipmentTypeUnsupported() throws Exception {
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class)))
                .thenThrow(new BookingValidationException("Unsupported equipment type: TANK"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported equipment type: TANK"));

        verify(bookingAccessAuthorizer).authorizeCreateCustomer(3001L);
        verify(bookingMapper, never()).toCreatedResponse(any());
    }

    @Test
    void shouldReturn422WhenScheduleNotAvailable() throws Exception {
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class)))
                .thenThrow(new ScheduleNotAvailableException("Schedule 1001 is not available"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Schedule 1001 is not available"));

        verify(bookingAccessAuthorizer).authorizeCreateCustomer(3001L);
        verify(bookingMapper, never()).toCreatedResponse(any());
    }

    @Test
    void shouldReturn422WhenQuoteNotValid() throws Exception {
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class)))
                .thenThrow(new QuoteNotValidException("Quote 2001 is not valid"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Quote 2001 is not valid"));

        verify(bookingAccessAuthorizer).authorizeCreateCustomer(3001L);
        verify(bookingMapper, never()).toCreatedResponse(any());
    }

    @Test
    void shouldListBookingsWithFiltersAndPagedResponse() throws Exception {
        Booking booking = pendingBooking();
        BookingResponse response = pendingBookingResponse();
        when(bookingService.getBookings(eq(3001L), eq(BookingStatus.PENDING), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(booking), invocation.getArgument(2), 5));
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("customerId", "3001")
                        .param("status", "PENDING")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.content[0].customerId").value(3001))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].scheduleId").value(1001))
                .andExpect(jsonPath("$.content[0].quoteId").value(2001))
                .andExpect(jsonPath("$.content[0].customer.name").value("Acme Shipping Co."))
                .andExpect(jsonPath("$.content[0].cargo.description").value("Industrial machinery parts"))
                .andExpect(jsonPath("$.content[0].equipment[0].type").value("20FT"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService, bookingMapper);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeListCustomer(3001L);
        orderedCalls.verify(bookingService).getBookings(eq(3001L), eq(BookingStatus.PENDING), pageable.capture());
        orderedCalls.verify(bookingMapper).toResponse(booking);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldReturn400WhenStatusQueryParameterInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .param("customerId", "3001")
                        .param("status", "LOST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'status' must be a valid BookingStatus"));

        verifyNoMoreInteractions(bookingAccessAuthorizer, bookingService, bookingMapper);
    }

    @Test
    void shouldApplyDefaultAndMaxPageSizeWhenListingBookings() throws Exception {
        when(bookingService.getBookings(isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(2), 0));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("size", "250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.last").value(true));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeListCustomer(null);
        orderedCalls.verify(bookingService).getBookings(isNull(), isNull(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldGetBookingByNumericId() throws Exception {
        Booking booking = savedBooking();
        BookingResponse response = bookingResponse();
        when(bookingService.getBookingById(42L)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.scheduleId").value(1001))
                .andExpect(jsonPath("$.quoteId").value(2001))
                .andExpect(jsonPath("$.equipment[0].type").value("20FT"))
                .andExpect(jsonPath("$.equipment[0].quantity").value(2));

        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService, bookingMapper);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeBookingAccess(42L);
        orderedCalls.verify(bookingService).getBookingById(42L);
        orderedCalls.verify(bookingMapper).toResponse(booking);
        verify(bookingService, never()).getBookingByReference(any());
    }

    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {
        when(bookingService.getBookingById(42L))
                .thenThrow(new BookingNotFoundException("Booking not found with id 42"));

        mockMvc.perform(get("/api/v1/bookings/42")
                        .header("X-Request-ID", "req-controller-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found with id 42"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/42"))
                .andExpect(jsonPath("$.requestId").value("req-controller-404"));

        verify(bookingAccessAuthorizer).authorizeBookingAccess(42L);
        verify(bookingMapper, never()).toResponse(any());
    }

    @Test
    void shouldGetBookingByReference() throws Exception {
        Booking booking = savedBooking();
        BookingResponse response = bookingResponse();
        when(bookingService.getBookingByReference("BKG-2026-00042")).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/BKG-2026-00042"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"));

        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService, bookingMapper);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeBookingAccess("BKG-2026-00042");
        orderedCalls.verify(bookingService).getBookingByReference("BKG-2026-00042");
        orderedCalls.verify(bookingMapper).toResponse(booking);
        verify(bookingService, never()).getBookingById(any());
    }

    @Test
    void shouldRejectInvalidBookingIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/not-a-booking"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid booking identifier: not-a-booking. "
                                + "Expected numeric ID or booking reference in format BKG-YYYY-NNNNN"
                ));

        verifyNoMoreInteractions(bookingAccessAuthorizer, bookingService, bookingMapper);
    }

    @Test
    void shouldCancelBookingAndReturn200() throws Exception {
        Booking cancelledBooking = bookingBuilder()
                .status(BookingStatus.CANCELLED)
                .updatedAt(Instant.parse("2026-05-19T14:00:00Z"))
                .build();
        BookingResponse response = bookingResponse("CANCELLED", Instant.parse("2026-05-19T14:00:00Z"));
        when(bookingService.cancelBooking(42L)).thenReturn(cancelledBooking);
        when(bookingMapper.toResponse(cancelledBooking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-19T14:00:00Z"));

        InOrder orderedCalls = inOrder(bookingAccessAuthorizer, bookingService, bookingMapper);
        orderedCalls.verify(bookingAccessAuthorizer).authorizeBookingAccess(42L);
        orderedCalls.verify(bookingService).cancelBooking(42L);
        orderedCalls.verify(bookingMapper).toResponse(cancelledBooking);
    }

    @Test
    void shouldReturn409WhenInvalidStateTransition() throws Exception {
        when(bookingService.cancelBooking(42L))
                .thenThrow(new IllegalStateTransitionException("Cannot transition booking from COMPLETED to CANCELLED"));

        mockMvc.perform(patch("/api/v1/bookings/42/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot transition booking from COMPLETED to CANCELLED"));

        verify(bookingAccessAuthorizer).authorizeBookingAccess(42L);
        verify(bookingMapper, never()).toResponse(any());
    }

    @Test
    void shouldConfirmBookingAndReturn200() throws Exception {
        Booking confirmedBooking = statusBooking(BookingStatus.CONFIRMED, "2026-05-19T14:30:00Z");
        BookingResponse response = bookingResponse("CONFIRMED", Instant.parse("2026-05-19T14:30:00Z"));
        when(bookingService.confirmBooking(42L)).thenReturn(confirmedBooking);
        when(bookingMapper.toResponse(confirmedBooking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.scheduleId").value(1001))
                .andExpect(jsonPath("$.quoteId").value(2001))
                .andExpect(jsonPath("$.customer.name").value("Acme Shipping Co."))
                .andExpect(jsonPath("$.cargo.description").value("Industrial machinery parts"))
                .andExpect(jsonPath("$.equipment[0].type").value("20FT"))
                .andExpect(jsonPath("$.equipment[0].quantity").value(2))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-19T14:30:00Z"));

        verify(bookingService).confirmBooking(42L);
        verify(bookingMapper).toResponse(confirmedBooking);
    }

    @Test
    void shouldStartBookingAndReturn200() throws Exception {
        Booking inProgressBooking = statusBooking(BookingStatus.IN_PROGRESS, "2026-05-19T15:00:00Z");
        BookingResponse response = bookingResponse("IN_PROGRESS", Instant.parse("2026-05-19T15:00:00Z"));
        when(bookingService.startBooking(42L)).thenReturn(inProgressBooking);
        when(bookingMapper.toResponse(inProgressBooking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.scheduleId").value(1001))
                .andExpect(jsonPath("$.quoteId").value(2001))
                .andExpect(jsonPath("$.customer.name").value("Acme Shipping Co."))
                .andExpect(jsonPath("$.cargo.description").value("Industrial machinery parts"))
                .andExpect(jsonPath("$.equipment[0].type").value("20FT"))
                .andExpect(jsonPath("$.equipment[0].quantity").value(2))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-19T15:00:00Z"));

        verify(bookingService).startBooking(42L);
        verify(bookingMapper).toResponse(inProgressBooking);
    }

    @Test
    void shouldCompleteBookingAndReturn200() throws Exception {
        Booking completedBooking = statusBooking(BookingStatus.COMPLETED, "2026-05-19T16:00:00Z");
        BookingResponse response = bookingResponse("COMPLETED", Instant.parse("2026-05-19T16:00:00Z"));
        when(bookingService.completeBooking(42L)).thenReturn(completedBooking);
        when(bookingMapper.toResponse(completedBooking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.bookingReference").value("BKG-2026-00042"))
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.scheduleId").value(1001))
                .andExpect(jsonPath("$.quoteId").value(2001))
                .andExpect(jsonPath("$.customer.name").value("Acme Shipping Co."))
                .andExpect(jsonPath("$.cargo.description").value("Industrial machinery parts"))
                .andExpect(jsonPath("$.equipment[0].type").value("20FT"))
                .andExpect(jsonPath("$.equipment[0].quantity").value(2))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-19T16:00:00Z"));

        verify(bookingService).completeBooking(42L);
        verify(bookingMapper).toResponse(completedBooking);
    }

    private CreateBookingRequest validRequest() {
        return new CreateBookingRequest(
                3001L,
                1001L,
                2001L,
                new CustomerRequest("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoRequest("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(
                        new EquipmentRequest("20FT", 2),
                        new EquipmentRequest("40HC", 1)
                )
        );
    }

    private Booking savedBooking() {
        return bookingBuilder()
                .status(BookingStatus.CONFIRMED)
                .updatedAt(Instant.parse("2026-05-19T13:00:00Z"))
                .build();
    }

    private BookingResponse bookingResponse() {
        return bookingResponse("CONFIRMED", Instant.parse("2026-05-19T13:00:00Z"));
    }

    private Booking pendingBooking() {
        return bookingBuilder()
                .status(BookingStatus.PENDING)
                .updatedAt(Instant.parse("2026-05-19T12:05:00Z"))
                .build();
    }

    private Booking statusBooking(BookingStatus status, String updatedAt) {
        return bookingBuilder()
                .status(status)
                .updatedAt(Instant.parse(updatedAt))
                .build();
    }

    private BookingResponse pendingBookingResponse() {
        return bookingResponse("PENDING", Instant.parse("2026-05-19T12:05:00Z"));
    }

    private Booking.BookingBuilder bookingBuilder() {
        return Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .customerId(3001L)
                .scheduleId(1001L)
                .quoteId(2001L)
                .customerName("Acme Shipping Co.")
                .customerEmail("logistics@acme.com")
                .customerPhone("+36-1-234-5678")
                .cargoDescription("Industrial machinery parts")
                .cargoWeightKg(new BigDecimal("12000.00"))
                .createdAt(Instant.parse("2026-05-19T12:00:00Z"));
    }

    private BookingResponse bookingResponse(String status, Instant updatedAt) {
        return new BookingResponse(
                42L,
                "BKG-2026-00042",
                3001L,
                status,
                1001L,
                2001L,
                new CustomerResponse("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoResponse("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(new EquipmentResponse("20FT", 2)),
                Instant.parse("2026-05-19T12:00:00Z"),
                updatedAt
        );
    }

    private PageableHandlerMethodArgumentResolver maxSizePageableResolver() {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        ));
        return resolver;
    }
}
