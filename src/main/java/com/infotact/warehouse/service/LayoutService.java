package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the physical layout of a warehouse.
 * Handles the structural hierarchy including Zones, Aisles, and individual Storage Bins.
 */
public interface LayoutService {

    /**
     * Defines and adds a new functional Zone to a specific warehouse.
     * @param request Data containing the zone name and parent warehouse ID.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the warehouse does not exist.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if a zone with the same name exists in that warehouse.
     */
    void addZoneToWarehouse(ZoneRequest request);

    /**
     * Adds a physical Aisle to an existing Zone.
     * @param request Data containing the aisle code and parent zone/warehouse IDs.
     * @throws com.infotact.warehouse.exception.BadRequestException if the zone does not belong to the specified warehouse.
     */
    void addAisleToZone(AisleRequest request);

    /**
     * Performs a bulk generation of Storage Bins based on a naming prefix and quantity.
     * @param request Configuration for bin generation (prefix, count, capacity, hierarchy IDs).
     * @throws com.infotact.warehouse.exception.IllegalOperationException if the generation sequence exceeds 999.
     */
    void bulkCreateBins(@Valid BulkBinRequest request);

    /**
     * Retrieves the complete structural tree of a warehouse (Zones > Aisles > Bins).
     * @param id The UUID of the warehouse.
     * @return A hierarchical representation of the warehouse layout.
     */
    WarehouseLayoutResponse getWarehouseLayout(String id);

    /**
     * Retrieves a paginated list of bins located within a specific aisle.
     * @param aisleId The UUID of the aisle.
     * @param pageable Pagination parameters.
     * @return A page of bin summaries.
     */
    Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable);
}