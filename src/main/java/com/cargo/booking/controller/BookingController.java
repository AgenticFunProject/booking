package com.cargo.booking.controller;

import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.dto.response.BookingResponse;
import com.cargo.booking.dto.response.PagedResponse;
import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.security.BookingAccessAuthorizer;
import com.cargo.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/bookings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Bookings")
@RequiredArgsConstructor
public class BookingController {

    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("\\d+");
    private static final Pattern BOOKING_REFERENCE_PATTERN = Pattern.compile("BKG-\\d{4}-\\d{5}");

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final BookingAccessAuthorizer bookingAccessAuthorizer;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new booking",
            description = "Creates a booking in PENDING status and returns HTTP 201 when the booking is accepted."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Successful creation",
                    content = @Content(schema = @Schema(implementation = BookingCreatedResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request body"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "422", description = "Schedule or quote validation failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingCreatedResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        bookingAccessAuthorizer.authorizeCreateCustomer(request.customerId());
        Booking booking = bookingService.createBooking(toServiceRequest(request));
        return bookingMapper.toCreatedResponse(booking);
    }

    @GetMapping
    @Operation(
            summary = "List bookings",
            description = "Lists bookings with optional customer and status filters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful retrieval",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public PagedResponse<BookingResponse> getBookings(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        bookingAccessAuthorizer.authorizeListCustomer(customerId);
        return PagedResponse.from(bookingService.getBookings(customerId, status, pageable)
                .map(bookingMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get booking by ID or reference",
            description = "Retrieves a booking by numeric ID or BKG-YYYY-NNNNN reference."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful retrieval",
                    content = @Content(schema = @Schema(implementation = BookingResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid booking identifier"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingResponse getBookingById(@PathVariable("id") String id) {
        Booking booking;

        if (NUMERIC_ID_PATTERN.matcher(id).matches()) {
            Long bookingId = parseBookingId(id);
            bookingAccessAuthorizer.authorizeBookingAccess(bookingId);
            booking = bookingService.getBookingById(bookingId);
        } else if (BOOKING_REFERENCE_PATTERN.matcher(id).matches()) {
            bookingAccessAuthorizer.authorizeBookingAccess(id);
            booking = bookingService.getBookingByReference(id);
        } else {
            throw invalidBookingIdentifier(id);
        }

        return bookingMapper.toResponse(booking);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancels a booking and returns HTTP 200 with the updated booking."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful update",
                    content = @Content(schema = @Schema(implementation = BookingResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingResponse cancelBooking(@PathVariable("id") Long id) {
        bookingAccessAuthorizer.authorizeBookingAccess(id);
        Booking booking = bookingService.cancelBooking(id);
        return bookingMapper.toResponse(booking);
    }

    @PatchMapping("/{id}/confirm")
    @Operation(
            summary = "Confirm a booking",
            description = "Confirms a pending booking, reserves equipment, and returns the updated booking."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful update",
                    content = @Content(schema = @Schema(implementation = BookingResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition"),
            @ApiResponse(responseCode = "503", description = "External equipment reservation unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingResponse confirmBooking(@PathVariable("id") Long id) {
        return bookingMapper.toResponse(bookingService.confirmBooking(id));
    }

    @PatchMapping("/{id}/start")
    @Operation(
            summary = "Mark booking as in progress",
            description = "Marks a confirmed booking as in progress and returns the updated booking."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful update",
                    content = @Content(schema = @Schema(implementation = BookingResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingResponse startBooking(@PathVariable("id") Long id) {
        return bookingMapper.toResponse(bookingService.startBooking(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(
            summary = "Mark booking as completed",
            description = "Marks an in-progress booking as completed and returns the updated booking."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful update",
                    content = @Content(schema = @Schema(implementation = BookingResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error - invalid request parameter"),
            @ApiResponse(responseCode = "401", description = "Authentication required when security is enabled"),
            @ApiResponse(responseCode = "403", description = "Authenticated caller lacks permission or ownership"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public BookingResponse completeBooking(@PathVariable("id") Long id) {
        return bookingMapper.toResponse(bookingService.completeBooking(id));
    }

    private com.cargo.booking.service.CreateBookingRequest toServiceRequest(CreateBookingRequest request) {
        return new com.cargo.booking.service.CreateBookingRequest(
                request.customerId(),
                request.scheduleId(),
                request.quoteId(),
                request.customer().name(),
                request.customer().email(),
                request.customer().phone(),
                request.cargo().description(),
                request.cargo().weightKg(),
                toServiceEquipmentRequests(request.equipment())
        );
    }

    private List<com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest> toServiceEquipmentRequests(
            List<EquipmentRequest> equipment
    ) {
        return equipment.stream()
                .map(line -> new com.cargo.booking.service.CreateBookingRequest.EquipmentLineRequest(
                        line.type(),
                        line.quantity()
                ))
                .toList();
    }

    private Long parseBookingId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException ex) {
            throw invalidBookingIdentifier(id, ex);
        }
    }

    private BookingValidationException invalidBookingIdentifier(String id) {
        return invalidBookingIdentifier(id, null);
    }

    private BookingValidationException invalidBookingIdentifier(String id, Throwable cause) {
        String message = "Invalid booking identifier: " + id
                + ". Expected numeric ID or booking reference in format BKG-YYYY-NNNNN";
        return cause == null
                ? new BookingValidationException(message)
                : new BookingValidationException(message, cause);
    }
}
