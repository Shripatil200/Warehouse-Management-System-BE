package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing physical warehouse facilities.
 * Handles the lifecycle of warehouses, including registration, location updates,
 * and operational status toggling.
 */
public interface WarehouseService {

    /**
     * Retrieves a paginated list of all warehouses.
     * @param pageable Pagination and sorting parameters.
     * @param includeInactive If true, returns all records; if false, filters for active facilities.
     * @return A page of warehouse response objects.
     */
    Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive);

    /**
     * Retrieves detailed information for a specific warehouse.
     * @param id The unique identifier (UUID) of the warehouse.
     * @return The found warehouse details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if no warehouse exists with the given ID.
     */
    WarehouseResponse getWarehouse(String id);

    /**
     * Updates an existing warehouse's name and location.
     * @param id The unique identifier of the warehouse to update.
     * @param request The updated data (name and location).
     * @return The updated warehouse details.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if no active warehouse is found.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if the new name is taken by another facility.
     */
    WarehouseResponse updateWarehouse(String id, @Valid WarehouseRequest request);

    /**
     * Enables a deactivated warehouse, making it available for operational use.
     * @param id The unique identifier of the warehouse.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the ID is invalid.
     */
    void activateWarehouse(String id);

    /**
     * Deactivates a warehouse, effectively removing it from active operational views.
     * @param id The unique identifier of the warehouse.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the ID is invalid.
     */
    void deactivateWarehouse(String id);

    /**
     * Registers a new warehouse facility in the system.
     * @param request Data containing the facility name and geographic location.
     * @return The created warehouse details.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if the warehouse name is already registered.
     */
    WarehouseResponse createWarehouse(@Valid WarehouseRequest request);
}