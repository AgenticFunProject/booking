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
import com.cargo.booking.exception.GlobalExceptionHandler;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
import com.cargo.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
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

class BookingSecurityIntegrationTestSupport {

    static final String AUTH_JWT_SECRET = "shared-auth-jwt-secret-at-least-256-bits";
    static final String ISSUER = "platform-auth";
    static final String AUDIENCE = "equipments-service";

    private BookingSecurityIntegrationTestSupport() {
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }

    static String token(Map<String, Object> claims) {
        return token(claims, ISSUER, AUDIENCE, Instant.now().plus(Duration.ofHours(1)));
    }

    static String token(Map<String, Object> claims, String issuer, String audience, Instant expiration) {
        return Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .audience().add(audience).and()
                .expiration(Date.from(expiration))
                .signWith(signingKey())
                .compact();
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

    private static SecretKey signingKey() {
        return Keys.hmacShaKeyFor(AUTH_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }
}

@WebMvcTest(controllers = BookingController.class)
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
        "app.security.jwt.secret=shared-auth-jwt-secret-at-least-256-bits"
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
        String expired = BookingSecurityIntegrationTestSupport.token(
                Map.of("sub", "user-123", "roles", List.of("CUSTOMER"), "customerId", 3001),
                BookingSecurityIntegrationTestSupport.ISSUER,
                BookingSecurityIntegrationTestSupport.AUDIENCE,
                Instant.now().minus(Duration.ofMinutes(1))
        );
        String wrongIssuer = BookingSecurityIntegrationTestSupport.token(
                Map.of("sub", "user-123", "roles", List.of("CUSTOMER"), "customerId", 3001),
                "wrong-issuer",
                BookingSecurityIntegrationTestSupport.AUDIENCE,
                Instant.now().plus(Duration.ofHours(1))
        );
        String wrongAudience = BookingSecurityIntegrationTestSupport.token(
                Map.of("sub", "user-123", "roles", List.of("CUSTOMER"), "customerId", 3001),
                BookingSecurityIntegrationTestSupport.ISSUER,
                "users-service",
                Instant.now().plus(Duration.ofHours(1))
        );

        mockMvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(expired)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(wrongIssuer)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(wrongAudience)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookingService, bookingMapper, bookingRepository);
    }

    @Test
    void shouldAcceptUsersStyleAdminTokenWithoutUsersIntrospection() throws Exception {
        String usersAdminToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "users-42",
                "name", "Users Admin",
                "role", "admin"
        ));
        when(bookingService.getBookings(isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Booking>(List.of(), invocation.getArgument(2), 0));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(usersAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(bookingService).getBookings(isNull(), isNull(), any(Pageable.class));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldEnforceRoleAllowAndDenyRules() throws Exception {
        String operatorToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "operator-1",
                "roles", List.of("OPERATOR")
        ));
        String customerToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "customer-1",
                "roles", List.of("CUSTOMER"),
                "customerId", 3001
        ));
        Booking confirmedBooking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.CONFIRMED);
        BookingResponse confirmedResponse = BookingSecurityIntegrationTestSupport.bookingResponse(3001L, "CONFIRMED");
        when(bookingService.confirmBooking(42L)).thenReturn(confirmedBooking);
        when(bookingMapper.toResponse(confirmedBooking)).thenReturn(confirmedResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(operatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3001L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/bookings/42/confirm")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(operatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldApplyCustomerOwnershipChecksBeforeServiceCalls() throws Exception {
        String customerToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "customer-1",
                "roles", List.of("CUSTOMER"),
                "customerId", 3001
        ));
        String customerWithoutClaimToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "customer-2",
                "roles", List.of("CUSTOMER")
        ));
        when(bookingRepository.findById(42L))
                .thenReturn(Optional.of(BookingSecurityIntegrationTestSupport.booking(3002L, BookingStatus.PENDING)));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("customerId query parameter is required for customer callers"));

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerWithoutClaimToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3001L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(3002L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/bookings/42")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerToken)))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).createBooking(any());
        verify(bookingService, never()).getBookingById(42L);
    }

    @Test
    void shouldAllowCustomerWhenOwnershipMatches() throws Exception {
        String customerToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "customer-1",
                "roles", List.of("CUSTOMER"),
                "customer_id", 3001
        ));
        Booking booking = BookingSecurityIntegrationTestSupport.booking(3001L, BookingStatus.CANCELLED);
        BookingResponse response = BookingSecurityIntegrationTestSupport.bookingResponse(3001L, "CANCELLED");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(BookingSecurityIntegrationTestSupport.booking(
                3001L,
                BookingStatus.PENDING
        )));
        when(bookingService.cancelBooking(42L)).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/42/cancel")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(3001))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldAllowServiceAndAdminToActForRequestedCustomers() throws Exception {
        String serviceToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "booking-worker",
                "roles", List.of("SERVICE")
        ));
        String adminToken = BookingSecurityIntegrationTestSupport.token(Map.of(
                "sub", "admin-1",
                "roles", List.of("ADMIN")
        ));
        Booking booking = BookingSecurityIntegrationTestSupport.booking(9001L, BookingStatus.PENDING);
        when(bookingService.createBooking(any(com.cargo.booking.service.CreateBookingRequest.class))).thenReturn(booking);
        when(bookingMapper.toCreatedResponse(booking))
                .thenReturn(BookingSecurityIntegrationTestSupport.createdResponse(9001L));
        when(bookingService.getBookings(isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Booking>(List.of(), invocation.getArgument(2), 0));

        mockMvc.perform(post("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(serviceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                BookingSecurityIntegrationTestSupport.createRequest(9001L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(9001));

        mockMvc.perform(get("/api/v1/bookings")
                        .header(HttpHeaders.AUTHORIZATION, BookingSecurityIntegrationTestSupport.bearer(adminToken)))
                .andExpect(status().isOk());

        verifyNoInteractions(bookingRepository);
    }
}

@WebMvcTest(controllers = BookingController.class)
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
        "app.security.jwt.secret=shared-auth-jwt-secret-at-least-256-bits"
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
}
