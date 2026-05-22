package com.cargo.booking.service;

import com.cargo.booking.client.EquipmentClient;
import com.cargo.booking.client.dto.EquipmentLineDTO;
import com.cargo.booking.client.QuoteClient;
import com.cargo.booking.client.ScheduleClient;
import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.exception.BookingValidationException;
import com.cargo.booking.exception.EquipmentReservationException;
import com.cargo.booking.exception.QuoteNotValidException;
import com.cargo.booking.exception.ScheduleNotAvailableException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import com.cargo.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingReferenceGenerator bookingReferenceGenerator;
    private final ScheduleClient scheduleClient;
    private final EquipmentClient equipmentClient;
    private final QuoteClient quoteClient;
    private final BookingStateMachine bookingStateMachine;

    public BookingService(
            BookingRepository bookingRepository,
            BookingReferenceGenerator bookingReferenceGenerator,
            ScheduleClient scheduleClient,
            EquipmentClient equipmentClient,
            QuoteClient quoteClient,
            BookingStateMachine bookingStateMachine
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingReferenceGenerator = bookingReferenceGenerator;
        this.scheduleClient = scheduleClient;
        this.equipmentClient = equipmentClient;
        this.quoteClient = quoteClient;
        this.bookingStateMachine = bookingStateMachine;
    }

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        validateCreateRequest(request);

        List<BookingEquipmentLine> equipmentLines = buildEquipmentLines(request.equipment());

        log.debug("Creating booking for scheduleId {}, quoteId {}, equipment line count {}",
                request.scheduleId(),
                request.quoteId(),
                equipmentLines.size()
        );

        if (!scheduleClient.validateSchedule(request.scheduleId())) {
            throw new ScheduleNotAvailableException(
                    "Schedule is not available for id: " + request.scheduleId()
            );
        }

        if (!quoteClient.validateQuote(request.quoteId(), request.scheduleId(), request.cargoWeightKg())) {
            throw new QuoteNotValidException(
                    "Quote is not valid for quoteId %d and scheduleId %d".formatted(
                            request.quoteId(),
                            request.scheduleId()
                    )
            );
        }

        Booking booking = Booking.builder()
                .bookingReference(bookingReferenceGenerator.generateReference())
                .status(BookingStatus.PENDING)
                .scheduleId(request.scheduleId())
                .quoteId(request.quoteId())
                .customerId(request.customerId())
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .customerPhone(request.customerPhone())
                .cargoDescription(request.cargoDescription())
                .cargoWeightKg(request.cargoWeightKg())
                .build();

        equipmentLines.forEach(line -> line.setBooking(booking));
        booking.getEquipmentLines().addAll(equipmentLines);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Created booking {}", savedBooking.getBookingReference());

        return savedBooking;
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findWithEquipmentLinesById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for id: " + id));
    }

    @Transactional(readOnly = true)
    public Booking getBookingByReference(String bookingReference) {
        return bookingRepository.findWithEquipmentLinesByBookingReference(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found for reference: " + bookingReference
                ));
    }

    @Transactional(readOnly = true)
    public Page<Booking> getBookings(Long customerId, BookingStatus status, Pageable pageable) {
        log.debug("Listing bookings with customerId filter present: {}, status: {}",
                customerId != null,
                status
        );

        if (customerId != null && status != null) {
            return initializeEquipmentLines(bookingRepository.findByCustomerIdAndStatus(customerId, status, pageable));
        }
        if (customerId != null) {
            return initializeEquipmentLines(bookingRepository.findByCustomerId(customerId, pageable));
        }
        if (status != null) {
            return initializeEquipmentLines(bookingRepository.findByStatus(status, pageable));
        }

        return initializeEquipmentLines(bookingRepository.findAll(pageable));
    }

    @Transactional
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findWithEquipmentLinesById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for id: " + id));
        BookingStatus currentStatus = booking.getStatus();

        bookingStateMachine.validateTransition(currentStatus, BookingStatus.CONFIRMED);
        equipmentClient.reserveEquipment(booking.getId(), toEquipmentLineDtos(booking.getEquipmentLines()));

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking {} transitioned from {} to {}",
                savedBooking.getBookingReference(),
                currentStatus,
                BookingStatus.CONFIRMED
        );

        return savedBooking;
    }

    @Transactional
    public Booking startBooking(Long id) {
        Booking booking = getBookingForLifecycleChange(id);
        BookingStatus currentStatus = booking.getStatus();

        bookingStateMachine.validateTransition(currentStatus, BookingStatus.IN_PROGRESS);
        booking.setStatus(BookingStatus.IN_PROGRESS);

        Booking savedBooking = bookingRepository.save(booking);
        logStateTransition(savedBooking, currentStatus, BookingStatus.IN_PROGRESS);

        return savedBooking;
    }

    @Transactional
    public Booking completeBooking(Long id) {
        Booking booking = getBookingForLifecycleChange(id);
        BookingStatus currentStatus = booking.getStatus();

        bookingStateMachine.validateTransition(currentStatus, BookingStatus.COMPLETED);
        booking.setStatus(BookingStatus.COMPLETED);

        Booking savedBooking = bookingRepository.save(booking);
        logStateTransition(savedBooking, currentStatus, BookingStatus.COMPLETED);

        return savedBooking;
    }

    @Transactional
    public Booking cancelBooking(Long id) {
        Booking booking = bookingRepository.findWithEquipmentLinesById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for id: " + id));
        BookingStatus currentStatus = booking.getStatus();

        bookingStateMachine.validateTransition(currentStatus, BookingStatus.CANCELLED);
        if (currentStatus == BookingStatus.CONFIRMED) {
            releaseReservedEquipment(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        logStateTransition(savedBooking, currentStatus, BookingStatus.CANCELLED);

        return savedBooking;
    }

    private Booking getBookingForLifecycleChange(Long id) {
        return bookingRepository.findWithEquipmentLinesById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for id: " + id));
    }

    private Page<Booking> initializeEquipmentLines(Page<Booking> bookings) {
        bookings.forEach(booking -> booking.getEquipmentLines().size());
        return bookings;
    }

    private void releaseReservedEquipment(Booking booking) {
        try {
            equipmentClient.releaseEquipment(booking.getId());
        } catch (EquipmentReservationException ex) {
            log.warn("Equipment release failed for cancelled booking {}: {}",
                    booking.getBookingReference(),
                    ex.getMessage()
            );
        }
    }

    private void logStateTransition(Booking booking, BookingStatus from, BookingStatus to) {
        log.info("Booking {} transitioned from {} to {}",
                booking.getBookingReference(),
                from,
                to
        );
    }

    private void validateCreateRequest(CreateBookingRequest request) {
        if (request == null) {
            throw new BookingValidationException("Booking request is required");
        }
        if (request.equipment() == null || request.equipment().isEmpty()) {
            throw new BookingValidationException("At least one equipment line is required");
        }
        validateRequired(request.customerId(), "Customer id is required");
        validateRequired(request.scheduleId(), "Schedule id is required");
        validateRequired(request.quoteId(), "Quote id is required");
        validateRequired(request.customerName(), "Customer name is required");
        validateRequired(request.customerEmail(), "Customer email is required");
        validateRequired(request.cargoDescription(), "Cargo description is required");
        validatePositiveWeight(request.cargoWeightKg());
    }

    private List<BookingEquipmentLine> buildEquipmentLines(
            List<CreateBookingRequest.EquipmentLineRequest> equipment
    ) {
        List<BookingEquipmentLine> lines = new ArrayList<>(equipment.size());

        for (CreateBookingRequest.EquipmentLineRequest lineRequest : equipment) {
            if (lineRequest == null) {
                throw new BookingValidationException("Equipment line must not be null");
            }
            if (lineRequest.quantity() < 1) {
                throw new BookingValidationException("Equipment quantity must be greater than zero");
            }

            EquipmentType type = parseEquipmentType(lineRequest.type());
            lines.add(BookingEquipmentLine.builder()
                    .type(type)
                    .quantity(lineRequest.quantity())
                    .build());
        }

        return lines;
    }

    private List<EquipmentLineDTO> toEquipmentLineDtos(List<BookingEquipmentLine> equipmentLines) {
        return equipmentLines.stream()
                .map(line -> new EquipmentLineDTO(line.getType().getCode(), line.getQuantity()))
                .toList();
    }

    private EquipmentType parseEquipmentType(String type) {
        try {
            return EquipmentType.fromCode(type);
        } catch (IllegalArgumentException ex) {
            throw new BookingValidationException("Unsupported equipment type: " + type, ex);
        }
    }

    private void validateRequired(Long value, String message) {
        if (value == null) {
            throw new BookingValidationException(message);
        }
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BookingValidationException(message);
        }
    }

    private void validatePositiveWeight(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BookingValidationException("Cargo weight must be greater than zero");
        }
    }
}
