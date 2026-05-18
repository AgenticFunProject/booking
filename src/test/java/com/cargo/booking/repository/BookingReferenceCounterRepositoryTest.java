package com.cargo.booking.repository;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Import(BookingReferenceCounterRepository.class)
class BookingReferenceCounterRepositoryTest {

    @Autowired
    private BookingReferenceCounterRepository bookingReferenceCounterRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnOneForNewYearAndAdvanceStoredNextValue() {
        Long sequence = bookingReferenceCounterRepository.getNextReferenceSeqForYear(2026);

        Object nextValue = entityManager
                .createNativeQuery("SELECT next_value FROM booking_reference_counters WHERE year = :year")
                .setParameter("year", 2026)
                .getSingleResult();

        assertThat(sequence).isEqualTo(1L);
        assertThat(((Number) nextValue).longValue()).isEqualTo(2L);
    }

    @Test
    void shouldReturnPreviousValueForExistingYear() {
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2027)).isEqualTo(1L);
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2027)).isEqualTo(2L);
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2027)).isEqualTo(3L);
    }

    @Test
    void shouldKeepCountersIndependentByYear() {
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2028)).isEqualTo(1L);
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2029)).isEqualTo(1L);
        assertThat(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2028)).isEqualTo(2L);
    }

    @Test
    void shouldCreateExpectedMigrationShape() {
        Object columnCount = entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.columns
                        WHERE table_name = 'booking_reference_counters'
                          AND column_name IN ('year', 'next_value', 'updated_at')
                        """)
                .getSingleResult();

        Object primaryKeyCount = entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE table_name = 'booking_reference_counters'
                          AND constraint_type = 'PRIMARY KEY'
                        """)
                .getSingleResult();

        assertThat(((Number) columnCount).longValue()).isEqualTo(3L);
        assertThat(((Number) primaryKeyCount).longValue()).isEqualTo(1L);
    }

    @Test
    void shouldAllocateUniqueSequencesConcurrently() throws Exception {
        int workers = 8;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(workers);
        Set<Long> sequences = new ConcurrentSkipListSet<>();

        try {
            var futures = IntStream.range(0, workers)
                    .mapToObj(index -> (Callable<Void>) () -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        sequences.add(bookingReferenceCounterRepository.getNextReferenceSeqForYear(2030));
                        return null;
                    })
                    .map(executorService::submit)
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executorService.shutdownNow();
        }

        assertThat(sequences).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    }
}
