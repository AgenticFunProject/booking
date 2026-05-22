package com.cargo.booking.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cargo.booking.config.SecurityConfig;
import com.cargo.booking.controller.BookingController;
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
import com.cargo.booking.exception.GlobalExceptionHandler;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
import com.cargo.booking.service.BookingService;
import com.cargo.booking.testutil.JwtTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class BookingSecurityIntegrationTestSupport {

    private BookingSecurityIntegrationTestSupport() {
    }

    static CreateBookingRequest createRequest(Long customerId) {
        return new CreateBookingRequest(
                customerId,
                1001L,
                2001L,
                new CustomerRequest("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoRequest("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(new EquipmentRequest("20FT", 2))
        );
    }

    static Booking booking(Long customerId, BookingStatus status) {
        return Booking.builder()
                .id(42L)
                .bookingReference("BKG-2026-00042")
                .customerId(customerId)
                .scheduleId(1001L)
                .quoteId(2001L)
                .customerName("Acme Shipping Co.")
                .customerEmail("logistics@acme.com")
                .customerPhone("+36-1-234-5678")
                .cargoDescription("Industrial machinery parts")
                .cargoWeightKg(new BigDecimal("12000.00"))
                .status(status)
                .createdAt(Instant.parse("2026-05-19T12:00:00Z"))
                .updatedAt(Instant.parse("2026-05-19T13:00:00Z"))
                .build();
    }

    static BookingCreatedResponse createdResponse(Long customerId) {
        return new BookingCreatedResponse(
                42L,
                "BKG-2026-00042",
                customerId,
                "PENDING",
                Instant.parse("2026-05-19T12:00:00Z")
        );
    }

    static BookingResponse bookingResponse(Long customerId, String status) {
        return new BookingResponse(
                42L,
                "BKG-2026-00042",
                customerId,
                status,
                1001L,
                2001L,
                new CustomerResponse("Acme Shipping Co.", "logistics@acme.com", "+36-1-234-5678"),
                new CargoResponse("Industrial machinery parts", new BigDecimal("12000.00")),
                List.of(new EquipmentResponse("20FT", 2)),
                Instant.parse("2026-05-19T12:00:00Z"),
                Instant.parse("2026-05-19T13:00:00Z")
        );
    }
}

@Tag("integration")
@WebMvcTest(controllers = {BookingController.class, SecurityRouteIntegrationTestController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtTokenProvider.class,
        BookingAccessAuthorizer.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.security.jwt.issuer=platform-auth",
        "app.security.jwt.audience=equipments-service",
        "app.security.jwt.secret=test-secret-key-that-is-at-least-256-bits-long"
})
class BookingSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingMapper bookingMapper;

    @MockitoBean
    private BookingRepository bookingRepository;

    @Test
    void shouldRejectMissingInvalidExpiredWrongIssuerAndWrongAudienceTokens() throws Exception {
        String expired = JwtTestHelper.generateExpiredToken("customer-3001");
        String wrongIssuer = JwtTestHelper.generateWrongIssuerToken("customer-3001");
        String wrongAudience = JwtTestHelper.generateWrongAudienceToken("customer-3001");
        String invalidSignature = JwtTestHelper.generateInvalidSignatureToken("customer-3001");

        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(JwtTestHelper.generateMalformedToken())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(expired)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(wrongIssuer)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(wrongAudience)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(invalidSignature)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingMapper, bookingRepository);
    }

    @Test
    void shouldPermitPublicEndpointsAndProtectMetricsWithRealJwtRoles() throws Exception {
        String operatorToken = JwtTestHelper.generateOperatorToken("operator-1");
        String adminToken = JwtTestHelper.generateAdminToken();

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api-docs/openapi.json"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(operatorToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        verifyNoInteractions(bookingService, bookingMapper, bookingRepository);
    }

    @Test
    void shouldAcceptUsersStyleAdminTokenWithoutUsersIntrospection() throws Exception {
        String usersAdminToken = JwtTestHelper.generateAdminToken();
        when(bookingService.getBookings(isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Booking>(List.of(), invocation.getArgument(2), 0));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(usersAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(bookingService).getBookings(isNull(), isNull(), any(Pageable.class));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldEnforceRoleAllowAndDenyRules() throws Exception {
        String operatorToken = JwtTestHelper.generateOperatorToken("operator-1");
        String customerToken = JwtTestHelper.generateCustomerToken(3001L);
        Booking confirmedBooking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.CONFIRMED);
        BookingResponse confirmedResponse = BookingSecurityIntegrationTestSupport.bookingResponse(3001L, "CONFIRMED");
        when(bookingService.confirmBooking(42L)).thenReturn(confirmedBooking);
        when(bookingMapper.toResponse(confirmedBooking)).thenReturn(confirmedResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3001L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldApplyCustomerOwnershipChecksBeforeServiceCalls() throws Exception {
        String customerToken = JwtTestHelper.generateCustomerToken(3001L);
        String customerWithoutClaimToken = JwtTestHelper.generateCustomerTokenMissingCustomerClaim("customer-2");
        when(bookingRepository.findById(42L))
                .thenReturn(Optional.of(BookingSecurityIntegrationTestSupport.booking(3002L, BookingStatus.PENDING)));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("customerId query parameter is required for customer callers"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerWithoutClaimToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3001L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3002L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/bookings/42")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken)))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).createBooking(any());
        verify(bookingService, never()).getBookingById(42L);
    }

    @Test
    void shouldApplySecurityStatusPrecedenceBeforeValidationAndServiceErrors() throws Exception {
        String customerWithoutClaimToken = JwtTestHelper.generateCustomerTokenMissingCustomerClaim("customer-2");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerWithoutClaimToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/bookings")
                        .param("customerId", "3001")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerWithoutClaimToken)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(bookingService, bookingMapper, bookingRepository);
    }

    @Test
    void shouldDeferMissingBookingToServiceNotFoundInsteadOfOwnershipDeny() throws Exception {
        String customerToken = JwtTestHelper.generateCustomerToken(3001L);
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());
        when(bookingService.getBookingById(404L))
                .thenThrow(new BookingNotFoundException("Booking not found with id 404"));

        mockMvc.perform(get("/api/v1/bookings/404")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found with id 404"));

        verify(bookingService).getBookingById(404L);
    }

    @Test
    void shouldAllowCustomerWhenOwnershipMatches() throws Exception {
        String customerToken = JwtTestHelper.generateCustomerTokenWithSnakeCaseClaim(3001L);
        Booking booking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.CANCELLED);
        BookingResponse response = BookingSecurityIntegrationTestSupport.bookingResponse(3001L, "CANCELLED");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(BookingSecurityIntegrationTestSupport.booking(
                3001L,
                BookingStatus.PENDING
        )));
        when(bookingService.cancelBooking(42L)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/cancel")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldAllowServiceAndAdminToActForRequestedCustomers() throws Exception {
        String serviceToken = JwtTestHelper.generateServiceToken("booking-worker");
        String adminToken = JwtTestHelper.generateAdminToken("admin-1");
        Booking booking = BookingSecurityIntegrationTestSupport.booking(9001L, BookingStatus.PENDING);
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class))).thenReturn(booking);
        when(bookingMapper.toCreatedResponse(booking))
                .thenReturn(BookingSecurityIntegrationTestSupport.createdResponse(9001L));
        when(bookingService.getBookings(isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Booking>(List.of(), invocation.getArgument(2), 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(serviceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(9001L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(9001));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearer(adminToken)))
                .andExpect(status().isOk());

        verifyNoInteractions(bookingRepository);
    }
}

@Tag("integration")
@WebMvcTest(controllers = {BookingController.class, SecurityRouteIntegrationTestController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtTokenProvider.class,
        BookingAccessAuthorizer.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "app.security.enabled=false",
        "app.security.jwt.secret=test-secret-key-that-is-at-least-256-bits-long"
})
class BookingSecurityDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private BookingMapper bookingMapper;

    @MockitoBean
    private BookingRepository bookingRepository;

    @Test
    void shouldPermitRequestsWithoutTokenAndUseRequestCustomerWhenSecurityIsDisabled() throws Exception {
        Booking booking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.PENDING);
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class))).thenReturn(booking);
        when(bookingMapper.toCreatedResponse(booking))
                .thenReturn(BookingSecurityIntegrationTestSupport.createdResponse(3001L));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3001L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(3001));

        verify(bookingService).createBooking(any(com.cargo.booking.service.CreateBookingRequest.class));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldListBookingsWithCustomerIdWhenSecurityIsDisabled() throws Exception {
        Booking booking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.PENDING);
        BookingResponse response = BookingSecurityIntegrationTestSupport.bookingResponse(3001L, "PENDING");
        when(bookingService.getBookings(any(), any(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Booking>(List.of(booking), invocation.getArgument(2), 1));
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("customerId", "3001")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(3001))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(bookingService).getBookings(any(), any(), any(Pageable.class));
        verifyNoInteractions(bookingRepository);
    }
}

@RestController
class SecurityRouteIntegrationTestController {

    @GetMapping({
            "/swagger-ui/index.html",
            "/api-docs/openapi.json",
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics"
    })
    String publicOrActuatorEndpoint() {
        return "ok";
    }
}
