package com.cargo.booking.client;

import com.cargo.booking.client.dto.EquipmentLineDTO;
import java.util.List;

public interface EquipmentClient {

    void reserveEquipment(Long bookingId, List<EquipmentLineDTO> equipment);

    void releaseEquipment(Long bookingId);
}
