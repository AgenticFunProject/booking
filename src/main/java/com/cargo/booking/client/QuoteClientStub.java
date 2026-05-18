package com.cargo.booking.client;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class QuoteClientStub implements QuoteClient {

    private static final Logger log = LoggerFactory.getLogger(QuoteClientStub.class);

    public QuoteClientStub() {
        log.warn("Using local stub implementation for quote client");
    }

    @Override
    public boolean validateQuote(Long quoteId, Long scheduleId, BigDecimal weightKg) {
        return true;
    }
}
