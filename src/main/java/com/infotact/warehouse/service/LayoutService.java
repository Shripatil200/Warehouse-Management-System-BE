package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LayoutService {
    void addZoneToWarehouse(@Valid ZoneRequest request);

    void addAisleToZone(@Valid AisleRequest request);

    void bulkCreateBins(@Valid BulkBinRequest request);

    WarehouseLayoutResponse getWarehouseLayout(String id);

    Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable);

}
