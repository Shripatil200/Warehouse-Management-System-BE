package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.dto.v1.request.SupplierRequest;
import com.infotact.warehouse.dto.v1.response.SupplierResponse;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.service.SupplierManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class SupplierManagementServiceImpl implements SupplierManagementService {

    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseContext warehouseContext;

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        if (supplierRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email already registered.");
        }
        if (supplierRepository.existsByContactNumber(request.getContactNumber())) {
            throw new AlreadyExistsException("Contact number already registered.");
        }

        String warehouseId = warehouseContext.getWarehouseId();
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));

        Supplier supplier = new Supplier();
        supplier.setWarehouse(warehouse);
        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setCompanyName(request.getCompanyName());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setAddress(request.getAddress());
        supplier.setWebsite(request.getWebsite());
        supplier.setStatus(SupplierStatus.ACTIVE);

        supplierRepository.save(supplier);
        log.info("Supplier created: {}", request.getEmail());
        return new SupplierResponse(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(String supplierId, SupplierRequest request) {
        String warehouseId = warehouseContext.getWarehouseId();
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> s.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));

        supplier.setName(request.getName());
        supplier.setEmail(request.getEmail());
        supplier.setContactNumber(request.getContactNumber());
        supplier.setCompanyName(request.getCompanyName());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setAddress(request.getAddress());
        supplier.setWebsite(request.getWebsite());

        return new SupplierResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getAllSuppliers(Pageable pageable) {
        String warehouseId = warehouseContext.getWarehouseId();
        return supplierRepository.findAllByWarehouseId(warehouseId, pageable).map(SupplierResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(String supplierId) {
        String warehouseId = warehouseContext.getWarehouseId();
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> s.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        return new SupplierResponse(supplier);
    }

    @Override
    @Transactional
    public void deactivateSupplier(String supplierId) {
        String warehouseId = warehouseContext.getWarehouseId();
        Supplier supplier = supplierRepository.findById(supplierId)
                .filter(s -> s.getWarehouse().getId().equals(warehouseId))
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        supplier.setStatus(SupplierStatus.INACTIVE);
        supplierRepository.save(supplier);
        log.info("Supplier deactivated: {}", supplierId);
    }
}
