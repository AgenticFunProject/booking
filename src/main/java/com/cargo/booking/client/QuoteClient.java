package com.cargo.booking.client;

import java.math.BigDecimal;

public interface QuoteClient {

    boolean validateQuote(Long quoteId, Long scheduleId, BigDecimal weightKg);
}
