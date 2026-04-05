package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.entity.PurchaseOrderItem;
import com.infotact.warehouse.entity.Supplier;
import com.infotact.warehouse.exception.EntityNotFoundException;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.PurchaseOrderRepository;
import com.infotact.warehouse.repository.SupplierRepository;
import com.infotact.warehouse.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link PurchaseOrderService}.
 * Handles business logic for validating suppliers/products and creating inbound orders.
 * Secured to ensure only Managers can execute business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')") // Secondary security layer for business logic
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderRequest request) {
        log.info("Processing creation of Purchase Order for supplier: {}", request.supplierId());

        // 1. Validate Supplier Existence
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        // 2. Initialize Purchase Order
        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setOrderDate(LocalDateTime.now());
        po.setStatus("PLACED");

        // 3. Map Request Items to Order Items while validating SKUs
        List<PurchaseOrderItem> items = request.items().stream().map(itemRequest -> {
            Product product = productRepository.findBySku(itemRequest.sku())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found: " + itemRequest.sku()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            return item;
        }).toList();

        po.setItems(items);

        log.info("Successfully created Purchase Order with {} items for supplier {}", items.size(), supplier.getName());
        return poRepository.save(po);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(String id) {
        log.info("Fetching Purchase Order with ID: {}", id);
        return poRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found with ID: " + id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchaseOrders(String status) {
        log.info("Fetching all Purchase Orders with status: {}", status);

        // If a status is provided, filter by it; otherwise, return all orders.
        if (status != null && !status.isBlank()) {
            return poRepository.findAllByStatus(status);
        }
        return poRepository.findAll();
    }
}