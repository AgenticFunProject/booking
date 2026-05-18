package com.cargo.booking.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BookingReferenceCounterRepository {

    private static final String NEXT_SEQUENCE_SQL = """
            INSERT INTO booking_reference_counters (year, next_value, updated_at)
            VALUES (:counterYear, 2, CURRENT_TIMESTAMP)
            ON CONFLICT (year)
            DO UPDATE SET
                next_value = booking_reference_counters.next_value + 1,
                updated_at = CURRENT_TIMESTAMP
            RETURNING next_value - 1
            """;

    private final EntityManager entityManager;

    public BookingReferenceCounterRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public Long getNextReferenceSeqForYear(int year) {
        Number nextSequence = (Number) entityManager
                .createNativeQuery(NEXT_SEQUENCE_SQL)
                .setParameter("counterYear", year)
                .getSingleResult();

        return nextSequence.longValue();
    }
}
