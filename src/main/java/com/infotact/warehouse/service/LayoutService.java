package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LayoutService {
    void addZoneToWarehouse(ZoneRequest request);

    void addAisleToZone(AisleRequest request);

    void bulkCreateBins(@Valid BulkBinRequest request);

    WarehouseLayoutResponse getWarehouseLayout(String id);

    Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable);

}
