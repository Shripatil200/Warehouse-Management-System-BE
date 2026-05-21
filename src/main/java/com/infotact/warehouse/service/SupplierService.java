package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for supplier identity management.
 * Covers self-registration and self-service profile management.
 */
public interface SupplierService {

    /**
     * Registers a new independent supplier account.
     * Public endpoint — no authentication required.
     *
     * @param request Registration details.
     * @return Confirmation message.
     */
    String register(SupplierRegistrationRequest request);

    /**
     * Retrieves the profile of the currently authenticated supplier.
     */
    SupplierResponse getMyProfile();

    /**
     * Updates the profile of the currently authenticated supplier.
     *
     * @param request Fields to update (all optional).
     * @return Confirmation message.
     */
    String updateMyProfile(SupplierProfileUpdateRequest request);

    /**
     * Returns a paginated list of all active suppliers.
     * Available to warehouse MANAGER and ADMIN for PO creation flows.
     *
     * @param pageable Pagination metadata.
     * @return Page of supplier profiles.
     */
    Page<SupplierResponse> getAllSuppliers(Pageable pageable);

    /**
     * Retrieves the public profile of a specific supplier by ID.
     *
     * @param supplierId Supplier user UUID.
     * @return Supplier profile.
     */
    SupplierResponse getById(String supplierId);
}
