package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.EquipmentType;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static com.cargo.booking.testutil.TestDataBuilder.aBooking;
import static com.cargo.booking.testutil.TestDataBuilder.anEquipmentLine;
import static com.cargo.booking.testutil.TestDataBuilder.anEquipmentLineFor;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.EMBEDDED)
@Tag("integration")
class BookingEquipmentLineRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingEquipmentLineRepository equipmentLineRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindByBookingId() {
        Booking booking = aBooking().bookingReference("BKG-2026-10001").build();
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
        booking.getEquipmentLines().add(anEquipmentLine()
                .booking(booking)
                .type(EquipmentType.FORTY_HC)
                .quantity(2)
                .build());
        Booking saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();

        assertThat(equipmentLineRepository.findByBookingId(saved.getId()))
                .hasSize(2)
                .extracting("type")
                .containsExactlyInAnyOrder(EquipmentType.TWENTY_FT, EquipmentType.FORTY_HC);
    }

    @Test
    void shouldDeleteByBookingId() {
        Booking booking = aBooking().bookingReference("BKG-2026-10002").build();
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
        Booking saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();

        equipmentLineRepository.deleteByBookingId(saved.getId());
        equipmentLineRepository.flush();
        entityManager.clear();

        assertThat(equipmentLineRepository.findByBookingId(saved.getId())).isEmpty();
        assertThat(bookingRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldKeepBookingAssociationLazy() {
        Booking booking = aBooking().bookingReference("BKG-2026-10003").build();
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
        Booking saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();

        var line = equipmentLineRepository.findByBookingId(saved.getId()).getFirst();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(line, "booking")).isFalse();
    }
}
