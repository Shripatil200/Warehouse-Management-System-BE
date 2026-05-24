package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ProductMasterRequest;
import com.infotact.warehouse.dto.v1.response.ProductMasterResponse;
import com.infotact.warehouse.entity.ProductCategory;
import com.infotact.warehouse.entity.ProductMaster;
import com.infotact.warehouse.exception.AlreadyExistsException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.ProductCategoryRepository;
import com.infotact.warehouse.repository.ProductMasterRepository;
import com.infotact.warehouse.service.ProductMasterService;
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
public class ProductMasterServiceImpl implements ProductMasterService {

    private final ProductMasterRepository   productMasterRepository;
    private final ProductCategoryRepository productCategoryRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ProductMasterResponse create(ProductMasterRequest request) {
        if (request.getBarcode() != null && productMasterRepository.existsByBarcode(request.getBarcode())) {
            throw new AlreadyExistsException(
                    "A ProductMaster with barcode '" + request.getBarcode() + "' already exists.");
        }
        ProductMaster pm = new ProductMaster();
        mapRequestToEntity(request, pm);
        return new ProductMasterResponse(productMasterRepository.save(pm));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ProductMasterResponse update(String id, ProductMasterRequest request) {
        ProductMaster pm = productMasterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductMaster not found."));

        if (request.getBarcode() != null
                && !request.getBarcode().equals(pm.getBarcode())
                && productMasterRepository.existsByBarcode(request.getBarcode())) {
            throw new AlreadyExistsException(
                    "A ProductMaster with barcode '" + request.getBarcode() + "' already exists.");
        }
        mapRequestToEntity(request, pm);
        return new ProductMasterResponse(productMasterRepository.save(pm));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductMasterResponse getById(String id) {
        return productMasterRepository.findById(id)
                .map(ProductMasterResponse::new)
                .orElseThrow(() -> new ResourceNotFoundException("ProductMaster not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductMasterResponse> search(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return productMasterRepository.findAllWithCategory(pageable).map(ProductMasterResponse::new);
        }
        return productMasterRepository.search(query.trim(), pageable).map(ProductMasterResponse::new);
    }

    private void mapRequestToEntity(ProductMasterRequest request, ProductMaster pm) {
        pm.setName(request.getName());
        pm.setDescription(request.getDescription());
        pm.setBarcode(request.getBarcode());
        pm.setUom(request.getUom());
        pm.setWeight(request.getWeight());
        pm.setLength(request.getLength());
        pm.setWidth(request.getWidth());
        pm.setHeight(request.getHeight());

        if (request.getCategoryId() != null) {
            ProductCategory cat = productCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
            pm.setCategory(cat);
        }
    }
}
