package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.entity.BookingEquipmentLine;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.model.enums.EquipmentType;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.EMBEDDED)
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindBookingById() {
        Booking saved = bookingRepository.saveAndFlush(aBooking("BKG-2026-00001"));

        entityManager.clear();

        assertThat(bookingRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(booking -> {
                    assertThat(booking.getBookingReference()).isEqualTo("BKG-2026-00001");
                    assertThat(booking.getCustomerId()).isEqualTo(3001L);
                    assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
                });
    }

    @Test
    void shouldFindByBookingReference() {
        bookingRepository.saveAndFlush(aBooking("BKG-2026-00002"));

        assertThat(bookingRepository.findByBookingReference("BKG-2026-00002"))
                .isPresent()
                .get()
                .extracting(Booking::getBookingReference)
                .isEqualTo("BKG-2026-00002");
    }

    @Test
    void shouldReturnEmptyForUnknownReference() {
        assertThat(bookingRepository.findByBookingReference("BKG-2026-99999")).isEmpty();
    }

    @Test
    void shouldFindByCustomerIdPaginated() {
        bookingRepository.save(aBooking("BKG-2026-00003", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00004", BookingStatus.CONFIRMED, 3001L, 1002L));
        bookingRepository.save(aBooking("BKG-2026-00005", BookingStatus.PENDING, 3002L, 1003L));
        bookingRepository.flush();

        assertThat(bookingRepository.findByCustomerId(3001L, PageRequest.of(0, 20)))
                .extracting("bookingReference")
                .containsExactlyInAnyOrder("BKG-2026-00003", "BKG-2026-00004");
    }

    @Test
    void shouldFindByCustomerIdAndStatus() {
        bookingRepository.save(aBooking("BKG-2026-00006", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00007", BookingStatus.CONFIRMED, 3001L, 1002L));
        bookingRepository.flush();

        assertThat(bookingRepository.findByCustomerIdAndStatus(3001L, BookingStatus.CONFIRMED, PageRequest.of(0, 20)))
                .extracting("bookingReference")
                .containsExactly("BKG-2026-00007");
    }

    @Test
    void shouldFindByStatus() {
        bookingRepository.save(aBooking("BKG-2026-00008", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00009", BookingStatus.CANCELLED, 3002L, 1002L));
        bookingRepository.flush();

        assertThat(bookingRepository.findByStatus(BookingStatus.CANCELLED, PageRequest.of(0, 20)))
                .extracting("bookingReference")
                .containsExactly("BKG-2026-00009");
    }

    @Test
    void shouldCheckExistsByBookingReference() {
        bookingRepository.saveAndFlush(aBooking("BKG-2026-00010"));

        assertThat(bookingRepository.existsByBookingReference("BKG-2026-00010")).isTrue();
        assertThat(bookingRepository.existsByBookingReference("BKG-2026-99999")).isFalse();
    }

    @Test
    void shouldFetchBookingWithEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00011");
        booking.getEquipmentLines().add(anEquipmentLine(booking));
        Booking saved = bookingRepository.saveAndFlush(booking);

        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(found, "equipmentLines")).isTrue();
        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldFetchBookingWithEquipmentLinesByReference() {
        Booking booking = aBooking("BKG-2026-00012");
        booking.getEquipmentLines().add(anEquipmentLine(booking));
        bookingRepository.saveAndFlush(booking);

        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesByBookingReference("BKG-2026-00012").orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(found, "equipmentLines")).isTrue();
        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldCascadeSaveEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00013");
        booking.getEquipmentLines().add(anEquipmentLine(booking));

        Booking saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();

        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldCascadeDeleteEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00014");
        booking.getEquipmentLines().add(anEquipmentLine(booking));
        Booking saved = bookingRepository.saveAndFlush(booking);

        Booking managed = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();
        managed.getEquipmentLines().clear();
        bookingRepository.saveAndFlush(managed);
        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();

        assertThat(found.getEquipmentLines()).isEmpty();
    }

    @Test
    void shouldFilterUsingSpecifications() {
        bookingRepository.save(aBooking("BKG-2026-00015", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00016", BookingStatus.CONFIRMED, 3001L, 1002L));
        bookingRepository.save(aBooking("BKG-2026-00017", BookingStatus.CONFIRMED, 3002L, 1002L));
        bookingRepository.flush();

        Specification<Booking> specification = BookingSpecification.hasCustomerId(3001L)
                .and(BookingSpecification.hasStatus(BookingStatus.CONFIRMED))
                .and(BookingSpecification.hasScheduleId(1002L));

        assertThat(bookingRepository.findAll(specification))
                .extracting(Booking::getBookingReference)
                .containsExactly("BKG-2026-00016");
    }

    private static Booking aBooking(String bookingReference) {
        return aBooking(bookingReference, BookingStatus.PENDING, 3001L, 1001L);
    }

    private static Booking aBooking(
            String bookingReference,
            BookingStatus status,
            Long customerId,
            Long scheduleId
    ) {
        return Booking.builder()
                .bookingReference(bookingReference)
                .status(status)
                .scheduleId(scheduleId)
                .quoteId(2001L)
                .customerId(customerId)
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .customerPhone("123456789")
                .cargoDescription("Test cargo")
                .cargoWeightKg(BigDecimal.valueOf(1000))
                .build();
    }

    private static BookingEquipmentLine anEquipmentLine(Booking booking) {
        return BookingEquipmentLine.builder()
                .booking(booking)
                .type(EquipmentType.TWENTY_FT)
                .quantity(1)
                .build();
    }
}
