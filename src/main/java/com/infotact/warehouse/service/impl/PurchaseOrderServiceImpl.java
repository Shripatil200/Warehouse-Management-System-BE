package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.PurchaseOrderRequest;
import com.infotact.warehouse.dto.v1.response.PurchaseOrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.PurchaseOrderService;
import com.infotact.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository    poRepository;
    private final ProductRepository          productRepository;
    private final SupplierRepository         supplierRepository;
    private final UserService                userService;

    @Override
    @Transactional
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        User manager      = userService.getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        // Resolve supplier from the dedicated suppliers table
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found with ID: " + request.supplierId()));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setWarehouse(manager.getWarehouse());
        po.setOrderDate(LocalDateTime.now());
        po.setExpectedDate(LocalDateTime.now().plusDays(7));
        po.setStatus(PurchaseOrderStatus.PENDING);

        List<PurchaseOrderItem> items = request.items().stream().map(itemRequest -> {
            Product product = productRepository
                    .findBySkuAndWarehouseIdAndActiveTrue(itemRequest.sku(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product SKU '" + itemRequest.sku() + "' not found in your facility."));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(po);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.unitCost());
            return item;
        }).collect(Collectors.toList());

        po.setItems(items);
        log.info("PO created for supplier '{}' at warehouse '{}'",
                supplier.getCompanyName(), manager.getWarehouse().getName());
        return mapToResponse(poRepository.save(po));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "purchaseOrders", key = "#id")
    public PurchaseOrderResponse getPurchaseOrder(String id) {
        User manager = userService.getAuthenticatedUser();
        PurchaseOrder po = poRepository.findByIdAndWarehouseId(id, manager.getWarehouse().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found or access denied."));
        return mapToResponse(po);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "purchaseOrders", key = "'list-' + #statusStr")
    public List<PurchaseOrderResponse> getAllPurchaseOrders(String statusStr) {
        User manager       = userService.getAuthenticatedUser();
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

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        BigDecimal totalValue = po.getItems().stream()
                .map(item -> item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurchaseOrderResponse.OrderItemDetail> itemDetails = po.getItems().stream()
                .map(item -> new PurchaseOrderResponse.OrderItemDetail(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getSku(),
                        item.getQuantity(),
                        item.getUnitCost(),
                        item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity()))
                )).collect(Collectors.toList());

        return new PurchaseOrderResponse(
                po.getId(),
                po.getSupplier().getName() + " (" + po.getSupplier().getCompanyName() + ")",
                po.getStatus().name(),
                po.getWarehouse().getName(),
                totalValue,
                po.getOrderDate(),
                po.getExpectedDate(),
                itemDetails
        );
    }
}
