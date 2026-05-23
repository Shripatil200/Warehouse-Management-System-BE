package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.SupplierLoginRequest;
import com.infotact.warehouse.dto.v1.request.SupplierProfileUpdateRequest;
import com.infotact.warehouse.dto.v1.request.SupplierRegistrationRequest;
import com.infotact.warehouse.dto.v1.response.AuthResponse;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    /** Public — registers a new supplier account. */
    String register(SupplierRegistrationRequest request);

    /** Public — authenticates a supplier and returns a JWT. */
    AuthResponse login(SupplierLoginRequest request);

    /** Authenticated supplier fetches their own profile. */
    SupplierResponse getMyProfile();

    /** Authenticated supplier updates their own profile. */
    String updateMyProfile(SupplierProfileUpdateRequest request);

    /** Warehouse MANAGER/ADMIN — paginated list of all suppliers. */
    Page<SupplierResponse> getAllSuppliers(Pageable pageable);

    /** Warehouse MANAGER/ADMIN — fetch a specific supplier by ID. */
    SupplierResponse getById(String supplierId);
}
