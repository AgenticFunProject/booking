package com.cargo.booking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cargo.booking.dto.request.CargoRequest;
import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.CustomerRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.dto.response.BookingCreatedResponse;
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
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
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
        return Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .customerId(3001L)
                .status(BookingStatus.PENDING)
                .createdAt(Instant.parse("2026-05-19T12:00:00Z"))
                .build();
    }
}
