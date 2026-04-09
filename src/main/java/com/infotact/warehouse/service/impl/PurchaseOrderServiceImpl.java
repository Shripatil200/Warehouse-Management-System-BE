package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.dto.v1.response.PurchaseOrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.exception.EntityNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link PurchaseOrderService} for inbound logistics management.
 * <p>
 * This service orchestrates the procurement process, allowing managers to forecast
 * incoming stock and track vendor fulfillment performance. It acts as the primary
 * ledger for "Expected Inventory" within a specific facility.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Resolves the current manager's profile to maintain facility-scoped data integrity.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("User profile not found."));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>Supplier Audit:</b> Verifies the vendor exists before initializing the contract.</li>
     * <li><b>Forecasting:</b> Automatically applies a default 7-day lead time for expected delivery.</li>
     * <li><b>Multi-tenant Scoping:</b> Anchors the PO directly to the manager's {@link Warehouse}.</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        User manager = getAuthenticatedUser();

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setWarehouse(manager.getWarehouse());
        po.setOrderDate(LocalDateTime.now());
        po.setExpectedDate(LocalDateTime.now().plusDays(7)); // Logic: Default supply chain lead time
        po.setStatus(PurchaseOrderStatus.PENDING);

        List<PurchaseOrderItem> items = request.items().stream().map(itemRequest -> {
            Product product = productRepository.findBySku(itemRequest.sku())
                    .orElseThrow(() -> new EntityNotFoundException("Product SKU not found: " + itemRequest.sku()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            return item;
        }).toList();

        po.setItems(items);
        log.info("Procurement: New PO created for Supplier '{}' at Warehouse '{}'", supplier.getName(), manager.getWarehouse().getName());
        return mapToResponse(poRepository.save(po));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Security Guardrail:</b> Ensures that a manager can only view POs
     * belonging strictly to their assigned facility.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "purchaseOrders", key = "#id")
    public PurchaseOrderResponse getPurchaseOrder(String id) {
        User manager = getAuthenticatedUser();
        PurchaseOrder po = poRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new EntityNotFoundException("Purchase Order not found or access denied."));
        return mapToResponse(po);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Operational View:</b> Provides filtered access to the procurement queue.
     * Results are cached by status to accelerate dashboard rendering.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "purchaseOrders", key = "'list-' + #statusStr")
    public List<PurchaseOrderResponse> getAllPurchaseOrders(String statusStr) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        List<PurchaseOrder> pos;
        if (statusStr != null && !statusStr.isBlank()) {
            PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(statusStr.toUpperCase());
            pos = poRepository.findAllByStatusAndWarehouseId(status, warehouseId);
        } else {
            pos = poRepository.findAllByWarehouseId(warehouseId);
        }

        return pos.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Maps the persistent PO entity to a detailed Response DTO.
     */
    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getId(),
                po.getSupplier().getName(),
                po.getStatus().name(),
                po.getWarehouse().getId(),
                po.getWarehouse().getName(),
                po.getOrderDate(),
                po.getExpectedDate(),
                po.getItems().stream()
                        .map(item -> new PurchaseOrderResponse.OrderItemDetail(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getProduct().getSku(),
                                item.getQuantity()
                        )).toList()
        );
    }
}