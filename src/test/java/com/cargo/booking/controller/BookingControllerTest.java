package com.cargo.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.cargo.booking.exception.GlobalExceptionHandler;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class BookingControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingMapper bookingMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService, bookingMapper))
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
        verify(bookingService).createBooking(serviceRequest.capture());
        verify(bookingMapper).toCreatedResponse(savedBooking);

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
        verify(bookingService).getBookings(eq(3001L), eq(BookingStatus.PENDING), pageable.capture());
        verify(bookingMapper).toResponse(booking);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
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
        verify(bookingService).getBookings(isNull(), isNull(), pageable.capture());

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

        verify(bookingService).getBookingById(42L);
        verify(bookingService, never()).getBookingByReference(any());
        verify(bookingMapper).toResponse(booking);
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

        verify(bookingService).getBookingByReference("BKG-2026-00042");
        verify(bookingService, never()).getBookingById(any());
        verify(bookingMapper).toResponse(booking);
    }

    @Test
    void shouldRejectInvalidBookingIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/not-a-booking"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid booking identifier: not-a-booking. "
                                + "Expected numeric ID or booking reference in format BKG-YYYY-NNNNN"
                ));

        verifyNoMoreInteractions(bookingService, bookingMapper);
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
