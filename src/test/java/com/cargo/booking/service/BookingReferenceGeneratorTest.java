package com.cargo.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cargo.booking.repository.BookingReferenceCounterRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingReferenceGeneratorTest {

    @Mock
    private BookingReferenceCounterRepository counterRepository;

    @Test
    void shouldGenerateReferenceUsingUtcYearAndPaddedSequence() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T23:59:59Z"), ZoneOffset.UTC);
        BookingReferenceGenerator generator = new BookingReferenceGenerator(counterRepository, clock);

        when(counterRepository.getNextReferenceSeqForYear(2026)).thenReturn(42L);

        String reference = generator.generateReference();

        assertThat(reference).isEqualTo("BKG-2026-00042");
        verify(counterRepository).getNextReferenceSeqForYear(2026);
    }
}
