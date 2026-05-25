package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.SupplierRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierManagementService {

    SupplierResponse createSupplier(SupplierRequest request);

    SupplierResponse updateSupplier(String supplierId, SupplierRequest request);

    Page<SupplierResponse> getAllSuppliers(Pageable pageable);

    SupplierResponse getById(String supplierId);

    void deactivateSupplier(String supplierId);
}
