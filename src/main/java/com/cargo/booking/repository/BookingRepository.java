package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.id = :id")
    Optional<Booking> findWithEquipmentLinesById(@Param("id") Long id);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.equipmentLines WHERE b.bookingReference = :reference")
    Optional<Booking> findWithEquipmentLinesByBookingReference(@Param("reference") String reference);
}
