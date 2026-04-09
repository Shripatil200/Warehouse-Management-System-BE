package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.CreateWarehouseRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Physical Infrastructure and Facility Onboarding.
 * <p>
 * This service acts as the root orchestrator for the platform's multi-tenant
 * architecture. It manages the lifecycle of physical facilities and handles
 * the initial provisioning of warehouse administrative accounts.
 * </p>
 */
public interface WarehouseService {

    /**
     * Retrieves a paginated list of facilities across the system.
     * <p>
     * <b>Administrative Usage:</b> Typically used by Super Admins to manage
     * the global facility portfolio.
     * </p>
     * @param pageable Pagination and sorting parameters.
     * @param includeInactive If true, includes decommissioned or suspended facilities.
     * @return A page of warehouse profiles.
     */
    Page<WarehouseResponse> getAllWarehouses(Pageable pageable, boolean includeInactive);

    /**
     * Retrieves detailed metadata for a specific facility.
     * @param id The unique identifier (UUID) of the warehouse.
     * @return Comprehensive warehouse details.
     */
    WarehouseResponse getWarehouse(String id);

    /**
     * Updates facility-level metadata (Name and Geographic Location).
     * <p>
     * <b>Validation:</b> Ensures the new name does not collide with existing
     * facilities to maintain reporting clarity.
     * </p>
     */
    WarehouseResponse updateWarehouse(String id, @Valid WarehouseRequest request);

    /**
     * Restores a facility to operational status.
     * <p>
     * Logic: Marking a warehouse as active allows users to once again log into
     * this facility and perform inventory transactions.
     * </p>
     */
    void activateWarehouse(String id);

    /**
     * Decommissions a facility (Soft-Delete).
     * <p>
     * <b>Safety Logic:</b> Suspends all operational activity for the facility.
     * Does not delete data, ensuring that historical audit logs for
     * stock and orders remain intact.
     * </p>
     */
    void deactivateWarehouse(String id);

    /**
     * THE ONBOARDING ENGINE: Facility and Admin Provisioning.
     * <p>
     * <b>Transactional Workflow:</b>
     * 1. <b>Facility Creation:</b> Validates name uniqueness and persists the new
     * {@link com.infotact.warehouse.entity.Warehouse} record.
     * 2. <b>Admin Initialization:</b> Creates the primary {@link com.infotact.warehouse.entity.User}
     * for the facility, enforcing the "One Admin per Warehouse" business rule.
     * 3. <b>Credential Logic:</b> Generates a deterministic welcome password
     * (Welcome@ + Last 4 digits of contact number).
     * 4. <b>Asynchronous Onboarding:</b> Triggers the Welcome Email workflow
     * containing the access credentials.
     * </p>
     * @param request Data containing facility attributes and the primary admin's profile.
     * @return The newly created warehouse details.
     * @throws com.infotact.warehouse.exception.AlreadyExistsException if the name or
     * admin email is already registered in the system.
     */
    WarehouseResponse createWarehouse(@Valid CreateWarehouseRequest request);
}