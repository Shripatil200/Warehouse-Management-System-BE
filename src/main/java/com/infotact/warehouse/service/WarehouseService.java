package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;

/**
 * Service interface for Warehouse lifecycle and tenant-scoped operations.
 *
 * <p>
 * This interface enforces strict single-tenant isolation:
 * Each authenticated user operates only within their assigned warehouse.
 * </p>
 */
public interface WarehouseService {

    /**
     * Retrieves the warehouse associated with the currently authenticated user.
     *
     * <p>
     * This replaces "get by ID" to enforce tenant isolation.
     * </p>
     *
     * @return The current tenant warehouse details.
     */
    WarehouseResponse getCurrentWarehouse();

    /**
     * Updates the current tenant warehouse metadata.
     *
     * <p>
     * Only ADMIN users are allowed to perform this operation.
     * </p>
     *
     * @param request Updated warehouse attributes.
     * @return Updated warehouse response.
     */
    WarehouseResponse updateWarehouse(@Valid WarehouseRequest request);

    /**
     * Activates the current tenant warehouse.
     *
     * <p>
     * Restores operational capability for the facility.
     * </p>
     */
    void activateWarehouse();

    /**
     * Deactivates the current tenant warehouse.
     *
     * <p>
     * Suspends all operations while preserving historical data.
     * </p>
     */
    void deactivateWarehouse();

    /**
     * Creates a new warehouse along with its primary admin user.
     *
     * <p>
     * This is a public onboarding operation and does not require authentication.
     * </p>
     *
     * @param request Warehouse and admin details.
     * @return Created warehouse details.
     */
    WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request);
}