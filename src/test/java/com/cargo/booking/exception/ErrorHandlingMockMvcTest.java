package com.cargo.booking.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.model.enums.BookingStatus;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ErrorHandlingMockMvcTest {

    private static final String REQUEST_ID = "req-123";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorProbeController(), new ApiFallbackController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    void shouldReturnStructuredBusinessErrorWithRequestId() throws Exception {
        mockMvc.perform(get("/test/errors/bookings/BKG-2026-00042")
                        .header(ErrorResponseBuilder.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found with reference BKG-2026-00042"))
                .andExpect(jsonPath("$.path").value("/test/errors/bookings/BKG-2026-00042"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID));

        mockMvc.perform(patch("/test/errors/state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot transition booking from COMPLETED to CANCELLED"));
    }

    @Test
    void shouldReturnSortedValidationViolationsWithRequestId() throws Exception {
        String requestBody = """
                {
                  "customerId": 3001,
                  "scheduleId": 1001,
                  "quoteId": 2001,
                  "customer": {
                    "name": "Acme Shipping Co.",
                    "email": "not-an-email",
                    "phone": "+36-1-234-5678"
                  },
                  "cargo": {
                    "description": "Industrial machinery parts",
                    "weightKg": -5
                  },
                  "equipment": [
                    { "type": "20FT", "quantity": 2 }
                  ]
                }
                """;

        mockMvc.perform(post("/test/errors/validate")
                        .header(ErrorResponseBuilder.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed with 2 error(s)"))
                .andExpect(jsonPath("$.path").value("/test/errors/validate"))
                .andExpect(jsonPath("$.requestId").value(REQUEST_ID))
                .andExpect(jsonPath("$.violations[0].field").value("cargo.weightKg"))
                .andExpect(jsonPath("$.violations[0].message").value("must be greater than or equal to 0.01"))
                .andExpect(jsonPath("$.violations[0].rejectedValue").value(-5))
                .andExpect(jsonPath("$.violations[1].field").value("customer.email"))
                .andExpect(jsonPath("$.violations[1].message").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.violations[1].rejectedValue").value("not-an-email"));
    }

    @Test
    void shouldReturnSafeMessagesForMalformedJsonEquipmentAndGenericFailures() throws Exception {
        mockMvc.perform(post("/test/errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"))
                .andExpect(jsonPath("$.message").value(not(containsString("JsonParseException"))))
                .andExpect(jsonPath("$.requestId").doesNotExist());

        mockMvc.perform(post("/test/errors/equipment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Equipment reservation is temporarily unavailable"))
                .andExpect(jsonPath("$.message").value(not(containsString("provider host timeout"))));

        mockMvc.perform(get("/test/errors/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
                .andExpect(jsonPath("$.message").value(not(containsString("database password"))));
    }

    @Test
    void shouldMapRepresentativeFrameworkAndSecurityExceptions() throws Exception {
        mockMvc.perform(get("/test/errors/status")
                        .param("status", "LOST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'status' must be a valid BookingStatus"))
                .andExpect(jsonPath("$.requestId").doesNotExist());

        mockMvc.perform(get("/test/errors/missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'customerId' is missing"));

        mockMvc.perform(delete("/test/errors/missing"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method 'DELETE' is not supported. Supported methods: GET"));

        mockMvc.perform(get("/test/errors/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void shouldReturnStandardJsonForUnknownApiPaths() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No endpoint found for GET /api/v1/unknown"))
                .andExpect(jsonPath("$.path").value("/api/v1/unknown"));
    }

    @RestController
    @RequestMapping(value = "/test/errors", produces = MediaType.APPLICATION_JSON_VALUE)
    private static class ErrorProbeController {

        @GetMapping("/bookings/{reference}")
        void bookingNotFound() {
            throw new BookingNotFoundException("Booking not found with reference BKG-2026-00042");
        }

        @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
        void validate(@Valid @RequestBody CreateBookingRequest request) {
        }

        @PostMapping("/equipment")
        void equipmentUnavailable() {
            throw new EquipmentReservationException("provider host timeout");
        }

        @GetMapping("/generic")
        void genericFailure() {
            throw new IllegalStateException("database password leaked in exception");
        }

        @GetMapping("/status")
        String status(@RequestParam BookingStatus status) {
            return status.name();
        }

        @GetMapping("/missing")
        String missing(@RequestParam String customerId) {
            return customerId;
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("customer mismatch");
        }

        @PatchMapping("/state")
        void illegalTransition() {
            throw new IllegalStateTransitionException("Cannot transition booking from COMPLETED to CANCELLED");
        }
    }

    @RestController
    private static class ApiFallbackController {

        @RequestMapping("/api/v1/{*path}")
        void unknownApiPath(HttpServletRequest request) throws NoResourceFoundException {
            throw new NoResourceFoundException(
                    HttpMethod.valueOf(request.getMethod()),
                    request.getRequestURI());
        }
    }
}
