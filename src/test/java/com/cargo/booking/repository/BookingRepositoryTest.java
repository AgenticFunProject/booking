package com.cargo.booking.repository;

import com.cargo.booking.model.entity.Booking;
import com.cargo.booking.model.enums.BookingStatus;
import com.cargo.booking.testutil.TestDataBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static com.cargo.booking.testutil.TestDataBuilder.aBooking;
import static com.cargo.booking.testutil.TestDataBuilder.anEquipmentLineFor;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.EMBEDDED)
@Tag("integration")
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
    void shouldFindByScheduleId() {
        bookingRepository.save(aBooking("BKG-2026-00010", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00011", BookingStatus.CONFIRMED, 3002L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00012", BookingStatus.PENDING, 3003L, 1002L));
        bookingRepository.flush();

        assertThat(bookingRepository.findByScheduleId(1001L))
                .extracting(Booking::getBookingReference)
                .containsExactlyInAnyOrder("BKG-2026-00010", "BKG-2026-00011");
    }

    @Test
    void shouldCheckExistsByBookingReference() {
        bookingRepository.saveAndFlush(aBooking("BKG-2026-00013"));

        assertThat(bookingRepository.existsByBookingReference("BKG-2026-00013")).isTrue();
        assertThat(bookingRepository.existsByBookingReference("BKG-2026-99999")).isFalse();
    }

    @Test
    void shouldCountByCustomerIdAndStatus() {
        bookingRepository.save(aBooking("BKG-2026-00014", BookingStatus.CONFIRMED, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00015", BookingStatus.CONFIRMED, 3001L, 1002L));
        bookingRepository.save(aBooking("BKG-2026-00016", BookingStatus.PENDING, 3001L, 1003L));
        bookingRepository.save(aBooking("BKG-2026-00017", BookingStatus.CONFIRMED, 3002L, 1004L));
        bookingRepository.flush();

        assertThat(bookingRepository.countByCustomerIdAndStatus(3001L, BookingStatus.CONFIRMED)).isEqualTo(2L);
    }

    @Test
    void shouldFetchBookingWithEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00018");
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
        Booking saved = bookingRepository.saveAndFlush(booking);

        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(found, "equipmentLines")).isTrue();
        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldFetchBookingWithEquipmentLinesByReference() {
        Booking booking = aBooking("BKG-2026-00019");
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
        bookingRepository.saveAndFlush(booking);

        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesByBookingReference("BKG-2026-00019").orElseThrow();
        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(found, "equipmentLines")).isTrue();
        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldCascadeSaveEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00020");
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));

        Booking saved = bookingRepository.saveAndFlush(booking);
        entityManager.clear();

        Booking found = bookingRepository.findWithEquipmentLinesById(saved.getId()).orElseThrow();

        assertThat(found.getEquipmentLines()).hasSize(1);
    }

    @Test
    void shouldCascadeDeleteEquipmentLines() {
        Booking booking = aBooking("BKG-2026-00021");
        booking.getEquipmentLines().add(anEquipmentLineFor(booking));
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
        bookingRepository.save(aBooking("BKG-2026-00022", BookingStatus.PENDING, 3001L, 1001L));
        bookingRepository.save(aBooking("BKG-2026-00023", BookingStatus.CONFIRMED, 3001L, 1002L));
        bookingRepository.save(aBooking("BKG-2026-00024", BookingStatus.CONFIRMED, 3002L, 1002L));
        bookingRepository.flush();

        Specification<Booking> specification = BookingSpecification.hasCustomerId(3001L)
                .and(BookingSpecification.hasStatus(BookingStatus.CONFIRMED))
                .and(BookingSpecification.hasScheduleId(1002L));

        assertThat(bookingRepository.findAll(specification))
                .extracting(Booking::getBookingReference)
                .containsExactly("BKG-2026-00023");
    }

    @Test
    void shouldFilterUsingCreatedAtSpecifications() {
        Instant beforeSave = Instant.now().minusSeconds(1);
        bookingRepository.saveAndFlush(aBooking("BKG-2026-00025"));
        Instant afterSave = Instant.now().plusSeconds(1);

        Specification<Booking> specification = BookingSpecification.createdAfter(beforeSave)
                .and(BookingSpecification.createdBefore(afterSave));

        assertThat(bookingRepository.findAll(specification))
                .extracting(Booking::getBookingReference)
                .contains("BKG-2026-00025");
    }

    @Test
    void shouldReturnNullForNullSpecificationParameters() {
        assertThat(BookingSpecification.hasCustomerId(null)).isNull();
        assertThat(BookingSpecification.hasStatus(null)).isNull();
        assertThat(BookingSpecification.hasScheduleId(null)).isNull();
        assertThat(BookingSpecification.createdAfter(null)).isNull();
        assertThat(BookingSpecification.createdBefore(null)).isNull();
    }

    @Test
    void shouldCreateExpectedBookingMigrationShape() {
        assertThat(countColumns(
                "bookings",
                "id",
                "booking_reference",
                "status",
                "schedule_id",
                "quote_id",
                "customer_id",
                "customer_name",
                "customer_email",
                "customer_phone",
                "cargo_description",
                "cargo_weight_kg",
                "created_at",
                "updated_at"
        )).isEqualTo(13L);
        assertThat(countIndexes("bookings", "idx_booking_reference", "idx_booking_customer_id",
                "idx_booking_status", "idx_booking_schedule_id")).isEqualTo(4L);
        assertThat(countCheckConstraints("bookings", "chk_bookings_status",
                "chk_bookings_cargo_weight_positive")).isEqualTo(2L);
    }

    @Test
    void shouldCreateExpectedEquipmentLineMigrationShape() {
        assertThat(countColumns("booking_equipment_lines", "id", "booking_id", "type", "quantity"))
                .isEqualTo(4L);
        assertThat(countIndexes("booking_equipment_lines", "idx_booking_equipment_lines_booking_id"))
                .isEqualTo(1L);
        assertThat(countCheckConstraints("booking_equipment_lines", "chk_booking_equipment_lines_type",
                "chk_booking_equipment_lines_quantity_positive")).isEqualTo(2L);
        assertThat(countForeignKeys("booking_equipment_lines", "fk_booking_equipment_lines_booking"))
                .isEqualTo(1L);
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
        return TestDataBuilder.aBooking()
                .bookingReference(bookingReference)
                .status(status)
                .scheduleId(scheduleId)
                .customerId(customerId)
                .build();
    }

    private long countColumns(String tableName, String... columnNames) {
        return ((Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = :tableName
                          AND column_name IN :columnNames
                        """)
                .setParameter("tableName", tableName)
                .setParameter("columnNames", java.util.List.of(columnNames))
                .getSingleResult()).longValue();
    }

    private long countIndexes(String tableName, String... indexNames) {
        return ((Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM pg_indexes
                        WHERE tablename = :tableName
                          AND indexname IN :indexNames
                        """)
                .setParameter("tableName", tableName)
                .setParameter("indexNames", java.util.List.of(indexNames))
                .getSingleResult()).longValue();
    }

    private long countCheckConstraints(String tableName, String... constraintNames) {
        return countConstraints(tableName, "CHECK", constraintNames);
    }

    private long countForeignKeys(String tableName, String... constraintNames) {
        return countConstraints(tableName, "FOREIGN KEY", constraintNames);
    }

    private long countConstraints(String tableName, String constraintType, String... constraintNames) {
        return ((Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE table_name = :tableName
                          AND constraint_type = :constraintType
                          AND constraint_name IN :constraintNames
                        """)
                .setParameter("tableName", tableName)
                .setParameter("constraintType", constraintType)
                .setParameter("constraintNames", java.util.List.of(constraintNames))
                .getSingleResult()).longValue();
    }
}
