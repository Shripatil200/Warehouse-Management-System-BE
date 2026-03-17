package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse.BinSummary;

import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {
    public Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive);

    WarehouseResponse getWarehouse(String id);

    WarehouseResponse updateWarehouse(String id, @Valid WarehouseRequest request);

    void activateWarehouse(String id);

    void deactivateWarehouse(String id);

    WarehouseResponse createWarehouse(@Valid WarehouseRequest request);


}
