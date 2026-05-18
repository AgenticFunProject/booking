package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByCustomerId(Long customerId, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    Page<Booking> findByCustomerIdAndStatus(Long customerId, BookingStatus status, Pageable pageable);

    List<Booking> findByScheduleId(Long scheduleId);

    boolean existsByBookingReference(String bookingReference);

    long countByCustomerIdAndStatus(Long customerId, BookingStatus status);
}
