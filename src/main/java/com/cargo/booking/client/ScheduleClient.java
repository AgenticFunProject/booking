package com.cargo.booking.client;

import com.cargo.booking.client.dto.ScheduleDTO;

public interface ScheduleClient {

    boolean validateSchedule(Long scheduleId);

    ScheduleDTO getScheduleDetails(Long scheduleId);
}
