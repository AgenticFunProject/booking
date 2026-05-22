package com.cargo.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.cargo.booking.dto.request.CargoRequest;
import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.CustomerRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.dto.response.BookingResponse;
import com.cargo.booking.dto.response.PagedResponse;
import com.cargo.booking.testutil.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Tag("e2e")
@ActiveProfiles({"test", "local"})
@TestPropertySource(properties = "app.security.enabled=false")
class BookingLifecycleE2ETest extends BaseIntegrationTest {

    private static final Long E2E_CUSTOMER_ID = 93001L;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteBookingLifecycleEndToEnd() throws JsonProcessingException {
        CreateBookingRequest request = validCreateBookingRequest();

        ResponseEntity<BookingCreatedResponse> created = createBooking(request);

        Long bookingId = created.getBody().id();

        ResponseEntity<BookingResponse> fetched = restTemplate.getForEntity(
                "/api/v1/bookings/{id}",
                BookingResponse.class,
                bookingId
        );

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().id()).isEqualTo(bookingId);
        assertThat(fetched.getBody().bookingReference()).isEqualTo(created.getBody().bookingReference());
        assertThat(fetched.getBody().status()).isEqualTo("PENDING");
        assertThat(fetched.getBody().equipment()).hasSize(1);

        BookingResponse confirmed = patchLifecycleEndpoint(bookingId, "confirm", "CONFIRMED");
        assertThat(confirmed.bookingReference()).isEqualTo(created.getBody().bookingReference());

        BookingResponse inProgress = patchLifecycleEndpoint(bookingId, "start", "IN_PROGRESS");
        assertThat(inProgress.bookingReference()).isEqualTo(created.getBody().bookingReference());

        BookingResponse completed = patchLifecycleEndpoint(bookingId, "complete", "COMPLETED");
        assertThat(completed.bookingReference()).isEqualTo(created.getBody().bookingReference());

        ResponseEntity<PagedResponse<BookingResponse>> listed = restTemplate.exchange(
                "/api/v1/bookings?customerId={customerId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                },
                E2E_CUSTOMER_ID
        );

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).isNotNull();
        assertThat(listed.getBody().content())
                .anySatisfy(booking -> {
                    assertThat(booking.id()).isEqualTo(bookingId);
                    assertThat(booking.bookingReference()).isEqualTo(created.getBody().bookingReference());
                    assertThat(booking.status()).isEqualTo("COMPLETED");
                    assertThat(booking.customerId()).isEqualTo(E2E_CUSTOMER_ID);
                });
    }

    @Test
    void shouldCancelBookingEndToEndAndRejectLaterConfirmation() throws JsonProcessingException {
        ResponseEntity<BookingCreatedResponse> created = createBooking(validCreateBookingRequest());
        Long bookingId = created.getBody().id();

        BookingResponse cancelled = patchLifecycleEndpoint(bookingId, "cancel", "CANCELLED");

        assertThat(cancelled.bookingReference()).isEqualTo(created.getBody().bookingReference());

        ResponseEntity<String> confirmAfterCancel = restTemplate.exchange(
                "/api/v1/bookings/{id}/confirm",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class,
                bookingId
        );

        assertThat(confirmAfterCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirmAfterCancel.getBody()).contains("CANCELLED to CONFIRMED");
    }

    private ResponseEntity<BookingCreatedResponse> createBooking(CreateBookingRequest request) {
        ResponseEntity<BookingCreatedResponse> created = restTemplate.postForEntity(
                "/api/v1/bookings",
                request,
                BookingCreatedResponse.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().id()).isNotNull();
        assertThat(created.getBody().bookingReference()).matches("BKG-\\d{4}-\\d{5}");
        assertThat(created.getBody().customerId()).isEqualTo(E2E_CUSTOMER_ID);
        assertThat(created.getBody().status()).isEqualTo("PENDING");
        return created;
    }

    private BookingResponse patchLifecycleEndpoint(
            Long bookingId,
            String action,
            String expectedStatus
    ) throws JsonProcessingException {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/bookings/{id}/{action}",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                String.class,
                bookingId,
                action
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BookingResponse body = objectMapper.readValue(response.getBody(), BookingResponse.class);
        assertThat(body.id()).isEqualTo(bookingId);
        assertThat(body.status()).isEqualTo(expectedStatus);
        assertThat(body.equipment()).hasSize(1);
        return body;
    }

    private CreateBookingRequest validCreateBookingRequest() {
        return new CreateBookingRequest(
                E2E_CUSTOMER_ID,
                TestDataBuilder.DEFAULT_SCHEDULE_ID,
                TestDataBuilder.DEFAULT_QUOTE_ID,
                new CustomerRequest(
                        TestDataBuilder.DEFAULT_CUSTOMER_NAME,
                        TestDataBuilder.DEFAULT_CUSTOMER_EMAIL,
                        TestDataBuilder.DEFAULT_CUSTOMER_PHONE
                ),
                new CargoRequest(
                        TestDataBuilder.DEFAULT_CARGO_DESCRIPTION,
                        TestDataBuilder.DEFAULT_CARGO_WEIGHT_KG
                ),
                List.of(new EquipmentRequest(
                        TestDataBuilder.DEFAULT_EQUIPMENT_TYPE,
                        TestDataBuilder.DEFAULT_EQUIPMENT_QUANTITY
                ))
        );
    }
}
