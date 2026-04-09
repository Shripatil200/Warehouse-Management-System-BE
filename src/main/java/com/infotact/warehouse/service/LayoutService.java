package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for physical facility modeling and layout orchestration.
 * <p>
 * This service manages the recursive structural hierarchy of the warehouse
 * (Zones -> Aisles -> Bins). It provides tools for both granular facility
 * configuration and high-volume infrastructure deployment.
 * </p>
 */
public interface LayoutService {

    /**
     * Segments the warehouse floor into functional Zones.
     * <p>
     * <b>Validation:</b> Zone names must be unique within the parent warehouse scope
     * (e.g., "Cold Storage", "Hazardous Materials").
     * </p>
     * @param request Data containing zone metadata and parent warehouse ID.
     */
    void addZoneToWarehouse(ZoneRequest request);

    /**
     * Maps a physical Aisle within a specific Zone.
     * <p>
     * <b>Security Logic:</b> Validates the structural hierarchy to ensure the
     * target zone belongs to the specified warehouse before insertion.
     * </p>
     * @param request Data containing the aisle code (e.g., "A-01") and parent zone ID.
     */
    void addAisleToZone(AisleRequest request);

    /**
     * THE GENERATION ENGINE: Automated Storage Bin Deployment.
     * <p>
     * <b>Operational Logic:</b>
     * <ul>
     * <li><b>Naming Convention:</b> Follows <code>[Prefix]-[Sequence]</code> (e.g., SHELF-001).</li>
     * <li><b>Idempotency:</b> Checks existing codes to prevent duplicate bin errors during retries.</li>
     * <li><b>Safety Ceiling:</b> Limits provisioning to 999 bins per request to protect DB performance.</li>
     * </ul>
     * </p>
     * @param request Configuration for prefix, quantity, and default capacity.
     */
    void bulkCreateBins(@Valid BulkBinRequest request);

    /**
     * Visualizes the facility's digital twin hierarchy.
     * <p>
     * <b>Usage:</b> Recursively fetches the tree structure. Designed for frontend map rendering.
     * <b>Aggregation:</b> Includes real-time capacity and occupancy metrics at every node.
     * </p>
     * @param id The UUID of the warehouse.
     * @return A hierarchical tree representation of the facility structure.
     */
    WarehouseLayoutResponse getWarehouseLayout(String id);

    /**
     * Provides a granular list view of storage locations within an aisle.
     * <p>
     * <b>Performance:</b> Uses pagination to handle high-density aisles (hundreds of bins)
     * without memory overhead.
     * </p>
     * @param aisleId  The UUID of the parent aisle.
     * @param pageable Pagination and sorting details (e.g., page number, size).
     * @return A paginated list of bin summaries.
     */
    Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable);

    /**
     * Toggles the operational status of a Warehouse Zone.
     * <p>
     * <b>Maintenance Gate:</b> Setting a zone to inactive logically "closes"
     * all nested aisles and bins, effectively blocking them from Putaway/Picking algorithms.
     * </p>
     * @param zoneId   The UUID of the zone.
     * @param isActive True for operational, False for maintenance/closed.
     */
    void updateZoneStatus(String zoneId, boolean isActive);

    /**
     * Toggles the operational status of a specific Aisle.
     * <p>
     * <b>Usage:</b> Temporarily block access to a specific row due to forklift
     * obstruction, shelf damage, or physical auditing.
     * </p>
     * @param aisleId  The UUID of the aisle.
     * @param isActive True for operational, False for blocked.
     */
    void updateAisleStatus(String aisleId, boolean isActive);

    /**
     * Toggles the operational status of a specific Storage Bin.
     * <p>
     * <b>Granular Control:</b> Used when a specific slot is damaged or needs
     * cleaning, while the rest of the aisle remains operational.
     * </p>
     * @param binId    The UUID of the bin.
     * @param isActive True for usable, False for under repair/blocked.
     */
    void updateBinStatus(String binId, boolean isActive);
}