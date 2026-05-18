package com.cargo.booking.client;

import com.cargo.booking.client.dto.ScheduleDTO;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class ScheduleClientStub implements ScheduleClient {

    private static final Logger log = LoggerFactory.getLogger(ScheduleClientStub.class);
    private static final Instant LOCAL_DEPARTURE_DATE = Instant.parse("2026-01-01T00:00:00Z");
    private static final String LOCAL_ROUTE_NAME = "LOCAL-STUB-ROUTE";
    private static final String OPEN_STATUS = "OPEN";

    public ScheduleClientStub() {
        log.warn("Using local stub implementation for schedule client");
    }

    @Override
    public boolean validateSchedule(Long scheduleId) {
        return true;
    }

    @Override
    public ScheduleDTO getScheduleDetails(Long scheduleId) {
        return new ScheduleDTO(scheduleId, LOCAL_ROUTE_NAME, LOCAL_DEPARTURE_DATE, OPEN_STATUS);
    }
}
