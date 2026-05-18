package com.cargo.booking.service;

import com.cargo.booking.exception.BookingNotFoundException;
import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.repository.BookingRepository;
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

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
            return bookingRepository.findByCustomerIdAndStatus(customerId, status, pageable);
        }
        if (customerId != null) {
            return bookingRepository.findByCustomerId(customerId, pageable);
        }
        if (status != null) {
            return bookingRepository.findByStatus(status, pageable);
        }

        return bookingRepository.findAll(pageable);
    }
}
