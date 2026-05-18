package com.cargo.booking.service;

import com.cargo.booking.repository.BookingReferenceCounterRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class BookingReferenceGenerator {

    private final BookingReferenceCounterRepository counterRepository;
    private final Clock clock;

    public BookingReferenceGenerator(BookingReferenceCounterRepository counterRepository) {
        this(counterRepository, Clock.systemUTC());
    }

    BookingReferenceGenerator(BookingReferenceCounterRepository counterRepository, Clock clock) {
        this.counterRepository = counterRepository;
        this.clock = clock;
    }

    public String generateReference() {
        int year = LocalDate.now(clock).getYear();
        Long sequence = counterRepository.getNextReferenceSeqForYear(year);

        return "BKG-%d-%05d".formatted(year, sequence);
    }
}
