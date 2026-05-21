package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.AuditAction;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.OrderRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Industry-Grade Order Fulfillment Service.
 * <p>
 * Key production features:
 * <ul>
 *     <li><b>Scan-Verified Packing:</b> Enforces physical matching of Bin and SKU before stock deduction.</li>
 *     <li><b>Audit Logging:</b> Records every physical scan attempt for accountability.</li>
 *     <li><b>Location-Aware Reservations:</b> Captures specific Bin IDs to guide pickers.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final LayoutService layoutService;
    private final BarcodeAuditService auditService;
    private final ConsignmentService consignmentService;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated profile not found"));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(OrderRequest request) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        log.info("Initiating Order {} - Reserving inventory via FEFO...", request.getOrderNumber());

        SellingOrder order = new SellingOrder();
        order.setOrderNumber(request.getOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setWarehouse(manager.getWarehouse());
        order.setCreatedAt(LocalDateTime.now());

        List<SellingOrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findBySkuAndWarehouseIdAndActiveTrue(itemReq.getSku(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("SKU " + itemReq.getSku() + " not found or inactive."));

            List<InventoryItem> reservedLayers = inventoryService.reserveStock(product.getId(), itemReq.getQuantity());

            for (InventoryItem layer : reservedLayers) {
                SellingOrderItem item = new SellingOrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(itemReq.getQuantity());
                item.setSellPriceAtTimeOfOrder(product.getSellingPrice());
                item.setSuggestedBinId(layer.getStorageBin().getId());
                item.setInventoryItemId(layer.getId());

                // ★ PROFIT TRACKING — snapshot cost and compute profit at order creation time
                item.setConsignment(product.isConsignment());
                if (product.isConsignment()) {
                    // Cost is zero for consignment; profit will be updated in verifyAndPack()
                    // once warehouseShare is known from the ConsignmentSale record.
                    item.setCostPriceAtTimeOfOrder(BigDecimal.ZERO);
                    item.setProfit(BigDecimal.ZERO);
                } else {
                    BigDecimal cost   = product.getCostPrice();
                    BigDecimal sell   = product.getSellingPrice();
                    BigDecimal profit = sell.subtract(cost)
                            .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                    item.setCostPriceAtTimeOfOrder(cost);
                    item.setProfit(profit);
                }

                orderItems.add(item);
            }
        }

        order.setItems(orderItems);
        return mapToResponse(orderRepository.save(order));
    }

    /**
     * Physical Scan Verification Engine with Audit Trail.
     * Logs every interaction to the Barcode Audit system.
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @CacheEvict(value = "orders", allEntries = true)
    public void verifyAndPack(String orderId, String scannedSku, String scannedBinCode) {
        User operator = getAuthenticatedUser();

        SellingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PICKING && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalOperationException("Order must be in PENDING or PICKING status for scan verification.");
        }

        for (SellingOrderItem item : order.getItems()) {
            try {
                // 1. Verify Product Barcode
                if (!item.getProduct().getSku().equalsIgnoreCase(scannedSku)) {
                    throw new IllegalOperationException("Scan Mismatch: Product SKU does not match.");
                }

                // 2. Verify physical location barcode
                boolean isCorrectBin = layoutService.verifyBinScan(scannedBinCode, item.getSuggestedBinId());
                if (!isCorrectBin) {
                    throw new IllegalOperationException("Location Error: Incorrect bin code scanned.");
                }

                // 3. Commit the pick
                inventoryService.commitPick(item.getInventoryItemId(), item.getQuantity());

                // AUDIT LOG: Success
                auditService.logSuccess(operator.getId(), item.getSuggestedBinId(),
                        orderId, AuditAction.PICKING, scannedBinCode);

            } catch (IllegalOperationException e) {
                // AUDIT LOG: Failure (Logs exactly what went wrong)
                auditService.logFailure(operator.getId(), item.getSuggestedBinId(),
                        orderId, AuditAction.PICKING, scannedBinCode, e.getMessage());
                throw e;
            }
        }

        order.setStatus(OrderStatus.PACKED);
        orderRepository.save(order);

        // ★ CONSIGNMENT + PROFIT PATCH — record sale and back-fill profit = warehouseShare
        for (SellingOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product.isConsignment()) {
                ConsignmentSale sale = consignmentService.recordConsignmentSale(item, product, LocalDateTime.now());
                if (sale != null) {
                    item.setProfit(sale.getWarehouseShare());
                }
            }
        }
        orderRepository.save(order);

        log.info("Order {} successfully verified and packed via handheld scan.", order.getOrderNumber());
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse updateOrderStatus(String orderId, OrderStatus nextStatus) {
        SellingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        switch (nextStatus) {
            case PICKING -> validateTransition(order.getStatus(), OrderStatus.PENDING);
            case PACKED -> throw new IllegalOperationException("PACKED status requires scanning via /verify-pack endpoint.");
            case SHIPPED -> validateTransition(order.getStatus(), OrderStatus.PACKED);
            case CANCELLED -> releaseInventory(order);
            default -> throw new IllegalOperationException("Transition not supported via manual update.");
        }

        order.setStatus(nextStatus);
        return mapToResponse(orderRepository.save(order));
    }

    private void releaseInventory(SellingOrder order) {
        for (SellingOrderItem item : order.getItems()) {
            inventoryService.releaseReservation(item.getInventoryItemId(), item.getQuantity());
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus expected) {
        if (current != expected) {
            throw new IllegalOperationException("State Error: Order must be " + expected + " before moving to " + current);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(String id) {
        User user = getAuthenticatedUser();
        return orderRepository.findById(id)
                .filter(o -> o.getWarehouse().getId().equals(user.getWarehouse().getId()))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getWarehouseOrders(String status) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        List<SellingOrder> orders = (status != null && !status.isBlank())
                ? orderRepository.findAllByWarehouseIdAndStatus(warehouseId, OrderStatus.valueOf(status.toUpperCase()))
                : orderRepository.findAllByWarehouseId(warehouseId);

        return orders.stream().map(this::mapToResponse).toList();
    }

    private OrderResponse mapToResponse(SellingOrder entity) {
        List<OrderResponse.OrderItemDetail> itemDetails = entity.getItems().stream()
                .map(item -> {
                    String displayBinCode = "N/A";
                    try {
                        if (item.getSuggestedBinId() != null) {
                            displayBinCode = layoutService.getBinCodeById(item.getSuggestedBinId());
                        }
                    } catch (Exception e) {
                        log.warn("Could not resolve bin code for ID: {}", item.getSuggestedBinId());
                    }

                    return OrderResponse.OrderItemDetail.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .sku(item.getProduct().getSku())
                            .quantity(item.getQuantity())
                            .sellPriceAtTimeOfOrder(item.getSellPriceAtTimeOfOrder())
                            .lineTotal(item.getSellPriceAtTimeOfOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .inventoryItemId(item.getInventoryItemId())
                            .suggestedBinId(item.getSuggestedBinId())
                            .binCode(displayBinCode)
                            .build();
                })
                .toList();

        return OrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .totalAmount(itemDetails.stream()
                        .map(OrderResponse.OrderItemDetail::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .warehouseName(entity.getWarehouse().getName())
                .items(itemDetails)
                .build();
    }
}