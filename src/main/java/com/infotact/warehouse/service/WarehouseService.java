package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;

/**
 * Service interface for the single-warehouse lifecycle.
 *
 * <p>This system supports exactly one warehouse. All authenticated users operate
 * within that warehouse; the warehouse ID is resolved from the JWT and stored in
 * {@link com.infotact.warehouse.config.WarehouseContext} for the duration of each
 * request. No operation accepts a warehouseId as an external parameter.</p>
 */
public interface WarehouseService {

    /**
     * Retrieves the warehouse associated with the currently authenticated user.
     *
     * @return The warehouse details.
     */
    WarehouseResponse getCurrentWarehouse();

    /**
     * Updates the warehouse name and location.
     * Only ADMIN users are permitted to call this.
     *
     * @param request Updated warehouse attributes.
     * @return Updated warehouse response.
     */
    WarehouseResponse updateWarehouse(@Valid WarehouseRequest request);

    /**
     * Activates the warehouse, restoring operational capability.
     */
    void activateWarehouse();

    /**
     * Deactivates the warehouse, suspending all operations while preserving
     * historical data.
     */
    void deactivateWarehouse();

    /**
     * Public bootstrap endpoint — creates the warehouse and its primary admin
     * user during first-time system setup.
     *
     * <p>Guarded by a single-warehouse check: if a warehouse already exists
     * an {@link com.infotact.warehouse.exception.AlreadyExistsException} is thrown.</p>
     *
     * @param request Warehouse and admin details, including OTP verification tokens.
     * @return Created warehouse details.
     */
    WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request);

    /**
     * Returns the ID of the single active warehouse.
     * Used internally by scheduled jobs that need to set the warehouse context
     * without an HTTP request / JWT present.
     *
     * @return Warehouse UUID.
     * @throws IllegalStateException if no warehouse has been set up yet.
     */
    String getSingleWarehouseId();
}
