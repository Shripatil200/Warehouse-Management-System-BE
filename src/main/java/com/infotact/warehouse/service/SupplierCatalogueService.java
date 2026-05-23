package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.SupplierProductRequest;
import com.infotact.warehouse.dto.v1.response.SupplierProductResponse;

import java.util.List;

/**
 * Service interface for managing a supplier's product catalogue.
 * <p>
 * Suppliers maintain their own list of {@link com.infotact.warehouse.entity.SupplierProduct}
 * offerings. Warehouse managers browse these offerings, sorted by price or lead time,
 * when selecting a supplier for a Purchase Order.
 * </p>
 */
public interface SupplierCatalogueService {

    /**
     * Adds a new product offering to the authenticated supplier's catalogue.
     *
     * @param request Product master reference, price, and lead time.
     * @return The created offering.
     */
    SupplierProductResponse addProduct(SupplierProductRequest request);

    /**
     * Updates the price or lead time of an existing offering.
     * Only the owning supplier may update their own offering.
     *
     * @param supplierProductId SupplierProduct UUID.
     * @param request           Updated price and lead time.
     * @return The updated offering.
     */
    SupplierProductResponse updateProduct(String supplierProductId, SupplierProductRequest request);

    /**
     * Deactivates an offering without deleting it, preserving PO history.
     *
     * @param supplierProductId SupplierProduct UUID.
     */
    void deactivateProduct(String supplierProductId);

    /**
     * Returns all active offerings in the authenticated supplier's catalogue.
     */
    List<SupplierProductResponse> getMyCatalogue();

    /**
     * Returns all active offerings for a given ProductMaster, from all suppliers.
     * Ordered by supply price ascending for side-by-side comparison.
     * Used by warehouse managers when raising a PO.
     *
     * @param productMasterId ProductMaster UUID.
     * @return List of offerings ordered by price.
     */
    List<SupplierProductResponse> getOffersForProduct(String productMasterId);
}
