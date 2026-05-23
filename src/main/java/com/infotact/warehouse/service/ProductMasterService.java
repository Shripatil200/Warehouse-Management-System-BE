package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.ProductMasterRequest;
import com.infotact.warehouse.dto.v1.response.ProductMasterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the global ProductMaster catalogue.
 * Suppliers create and maintain ProductMaster records.
 * Warehouse managers browse them when raising Purchase Orders.
 */
public interface ProductMasterService {

    /**
     * Creates a new global product definition.
     * Called by suppliers when listing a new product type.
     *
     * @param request Product definition details.
     * @return The created product master record.
     */
    ProductMasterResponse create(ProductMasterRequest request);

    /**
     * Updates an existing ProductMaster record.
     * Restricted to the supplier who created it or an ADMIN.
     *
     * @param id      ProductMaster UUID.
     * @param request Updated details.
     * @return The updated record.
     */
    ProductMasterResponse update(String id, ProductMasterRequest request);

    /**
     * Retrieves a single ProductMaster by ID.
     *
     * @param id ProductMaster UUID.
     * @return The product definition.
     */
    ProductMasterResponse getById(String id);

    /**
     * Searches the global product catalogue with optional name/barcode query.
     *
     * @param query    Optional search term (name or barcode).
     * @param pageable Pagination metadata.
     * @return Page of matching product definitions.
     */
    Page<ProductMasterResponse> search(String query, Pageable pageable);
}
