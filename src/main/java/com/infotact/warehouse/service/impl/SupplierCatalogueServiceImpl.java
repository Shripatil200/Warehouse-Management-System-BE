package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.SupplierProductRequest;
import com.infotact.warehouse.dto.v1.response.SupplierProductResponse;
import com.infotact.warehouse.entity.ProductMaster;
import com.infotact.warehouse.entity.SupplierProduct;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.ProductMasterRepository;
import com.infotact.warehouse.repository.SupplierProductRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.SupplierCatalogueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierCatalogueServiceImpl implements SupplierCatalogueService {

    private final SupplierProductRepository supplierProductRepository;
    private final ProductMasterRepository   productMasterRepository;
    private final UserRepository            userRepository;

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPPLIER')")
    public SupplierProductResponse addProduct(SupplierProductRequest request) {
        User supplier = getAuthenticatedUser();

        if (supplierProductRepository.existsByProductMasterIdAndSupplierId(
                request.getProductMasterId(), supplier.getId())) {
            throw new AlreadyExistsException("You have already listed this product. Update the existing entry instead.");
        }

        ProductMaster pm = productMasterRepository.findById(request.getProductMasterId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductMaster not found."));

        SupplierProduct sp = new SupplierProduct();
        sp.setProductMaster(pm);
        sp.setSupplier(supplier);
        sp.setSupplyPrice(request.getSupplyPrice());
        sp.setLeadTimeDays(request.getLeadTimeDays());
        sp.setActive(true);

        return new SupplierProductResponse(supplierProductRepository.save(sp));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPPLIER')")
    public SupplierProductResponse updateProduct(String supplierProductId, SupplierProductRequest request) {
        SupplierProduct sp = getOwnedSupplierProduct(supplierProductId);
        sp.setSupplyPrice(request.getSupplyPrice());
        sp.setLeadTimeDays(request.getLeadTimeDays());
        return new SupplierProductResponse(supplierProductRepository.save(sp));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('SUPPLIER')")
    public void deactivateProduct(String supplierProductId) {
        SupplierProduct sp = getOwnedSupplierProduct(supplierProductId);
        sp.setActive(false);
        supplierProductRepository.save(sp);
        log.info("Supplier product deactivated: {}", supplierProductId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPPLIER')")
    public List<SupplierProductResponse> getMyCatalogue() {
        User supplier = getAuthenticatedUser();
        return supplierProductRepository.findBySupplierIdAndActiveTrue(supplier.getId())
                .stream()
                .map(SupplierProductResponse::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<SupplierProductResponse> getOffersForProduct(String productMasterId) {
        if (!productMasterRepository.existsById(productMasterId)) {
            throw new ResourceNotFoundException("ProductMaster not found.");
        }
        return supplierProductRepository.findAllByProductMasterOrderedByPrice(productMasterId)
                .stream()
                .map(SupplierProductResponse::new)
                .toList();
    }

    private SupplierProduct getOwnedSupplierProduct(String id) {
        User supplier = getAuthenticatedUser();
        SupplierProduct sp = supplierProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupplierProduct not found."));
        if (!sp.getSupplier().getId().equals(supplier.getId())) {
            throw new UnauthorizedException("You do not own this product listing.");
        }
        return sp;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
