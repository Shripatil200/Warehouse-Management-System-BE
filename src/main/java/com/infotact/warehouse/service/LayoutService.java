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
 *
 * <p>
 * This service manages the structural hierarchy of a warehouse:
 * Zones → Aisles → Storage Bins.
 * </p>
 *
 * <p>
 * <b>Warehouse-Scoped Design:</b>
 * All operations are strictly scoped to the authenticated tenant (warehouse)
 * via WarehouseContext. No method accepts a warehouseId externally.
 * </p>
 */
public interface LayoutService {

    /**
     * Creates a functional Zone within the current warehouse.
     *
     * <p>
     * <b>Validation:</b>
     * Zone names must be unique within the warehouse scope.
     * </p>
     *
     * @param request Zone metadata (name, type).
     */
    void addZoneToWarehouse(ZoneRequest request);

    /**
     * Creates an Aisle within a Zone.
     *
     * <p>
     * <b>Security:</b>
     * Validates that the provided zone belongs to the current warehouse.
     * </p>
     *
     * @param request Aisle configuration.
     */
    void addAisleToZone(AisleRequest request);

    /**
     * Bulk-creates storage bins within an aisle.
     *
     * <p>
     * <b>Operational Logic:</b>
     * <ul>
     *     <li>Naming format: PREFIX-001, PREFIX-002...</li>
     *     <li>Ensures uniqueness within tenant scope</li>
     *     <li>Limits to 999 bins per request</li>
     * </ul>
     * </p>
     *
     * @param request Bin generation configuration.
     */
    void bulkCreateBins(@Valid BulkBinRequest request);

    /**
     * Retrieves the full warehouse layout for the current tenant.
     *
     * <p>
     * <b>Usage:</b> Used for frontend visualization (tree/map view).
     * </p>
     *
     * <p>
     * <b>Aggregation:</b>
     * Includes real-time metrics such as:
     * <ul>
     *     <li>Total capacity</li>
     *     <li>Current occupancy</li>
     *     <li>Bin counts</li>
     * </ul>
     * </p>
     *
     * @return Hierarchical layout response.
     */
    WarehouseLayoutResponse getWarehouseLayout();

    /**
     * Retrieves paginated bins for a given aisle.
     *
     * <p>
     * <b>Performance:</b>
     * Supports pagination for high-density aisles.
     * </p>
     *
     * @param aisleId Aisle identifier (validated for tenant ownership).
     * @param pageable Pagination configuration.
     * @return Paginated bin summaries.
     */
    Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable);

    /**
     * Updates the active status of a Zone.
     *
     * <p>
     * Inactive zones block all operations inside them.
     * </p>
     */
    void updateZoneStatus(String zoneId, boolean isActive);

    /**
     * Updates the active status of an Aisle.
     */
    void updateAisleStatus(String aisleId, boolean isActive);

    /**
     * Updates the active status of a Storage Bin.
     */
    void updateBinStatus(String binId, boolean isActive);

    /**
     * Generates barcode image for a bin.
     *
     * <p>
     * Used for printing physical labels.
     * </p>
     */
    byte[] getBinBarcode(String binId);

    /**
     * Verifies scanned barcode against expected bin.
     *
     * @return true if valid scan
     */
    boolean verifyBinScan(String scannedCode, String expectedBinId);

    /**
     * Retrieves bin code from its UUID.
     */
    String getBinCodeById(String binId);
}