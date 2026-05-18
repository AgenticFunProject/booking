package com.cargo.booking.repository;

import com.cargo.booking.model.entity.BookingEquipmentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookingEquipmentLineRepository extends JpaRepository<BookingEquipmentLine, Long> {

    List<BookingEquipmentLine> findByBookingId(Long bookingId);

    @Modifying
    @Transactional
    void deleteByBookingId(Long bookingId);
}
