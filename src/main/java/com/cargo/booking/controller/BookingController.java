package com.cargo.booking.controller;

import com.cargo.booking.dto.request.CreateBookingRequest;
import com.cargo.booking.dto.request.EquipmentRequest;
import com.cargo.booking.dto.response.BookingCreatedResponse;
import com.cargo.booking.mapper.BookingMapper;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/bookings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

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
        Booking booking = bookingService.createBooking(toServiceRequest(request));
        return bookingMapper.toCreatedResponse(booking);
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
}
