package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import jakarta.validation.Valid;

public interface InventoryService {
    void receiveShipment(@Valid ReceivingRequest request);
}
