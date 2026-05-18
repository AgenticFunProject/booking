package com.cargo.booking.client;

import com.cargo.booking.client.dto.EquipmentLineDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class EquipmentClientStub implements EquipmentClient {

    private static final Logger log = LoggerFactory.getLogger(EquipmentClientStub.class);

    public EquipmentClientStub() {
        log.warn("Using local stub implementation for equipment client");
    }

    @Override
    public void reserveEquipment(Long bookingId, List<EquipmentLineDTO> equipment) {
        log.info("Local equipment reservation stub accepted bookingId={} lineCount={}",
                bookingId,
                equipment == null ? 0 : equipment.size());
    }

    @Override
    public void releaseEquipment(Long bookingId) {
        log.info("Local equipment release stub accepted bookingId={}", bookingId);
    }
}
