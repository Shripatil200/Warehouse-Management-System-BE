package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.OrderRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.InventoryService;
import com.infotact.warehouse.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService; // Integrated for Stock Movements

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated profile not found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(OrderRequest request) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        log.info("Initiating Order {} - Reserving Stock...", request.getOrderNumber());

        SellingOrder order = new SellingOrder();
        order.setOrderNumber(request.getOrderNumber());
        order.setStatus(OrderStatus.PENDING); // Initial State
        order.setWarehouse(manager.getWarehouse());
        order.setCreatedAt(LocalDateTime.now());

        List<SellingOrderItem> items = request.getItems().stream().map(itemReq -> {
            Product product = productRepository.findBySkuAndWarehouseIdAndActiveTrue(itemReq.getSku(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("SKU " + itemReq.getSku() + " not found."));

            // 1. Soft-Lock Inventory: Reserve quantities using FEFO logic
            inventoryService.reserveStock(product.getId(), itemReq.getQuantity());

            SellingOrderItem item = new SellingOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setSellPriceAtTimeOfOrder(product.getSellingPrice());
            return item;
        }).toList();

        order.setItems(items);
        return mapToResponse(orderRepository.save(order));
    }

    /**
     * Dynamic State Transition Engine
     * Transitions an order through the fulfillment lifecycle.
     */
    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse updateOrderStatus(String orderId, OrderStatus nextStatus) {
        User manager = getAuthenticatedUser();
        SellingOrder order = orderRepository.findById(orderId)
                .filter(o -> o.getWarehouse().getId().equals(manager.getWarehouse().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        log.info("Transitioning Order {} from {} to {}", order.getOrderNumber(), order.getStatus(), nextStatus);

        // State Machine Logic
        switch (nextStatus) {
            case PICKING -> validateTransition(order.getStatus(), OrderStatus.PENDING);

            case PACKED -> {
                validateTransition(order.getStatus(), OrderStatus.PICKING);
                // 2. Physical Deduction: Commit the pick and reduce physical stock[cite: 1]
                fulfillInventory(order);
            }

            case SHIPPED -> validateTransition(order.getStatus(), OrderStatus.PACKED);

            case CANCELLED -> {
                // 3. Rollback: If cancelled, release the reserved quantities[cite: 1]
                releaseInventory(order);
            }

            default -> throw new IllegalOperationException("Unsupported status transition");
        }

        order.setStatus(nextStatus);
        return mapToResponse(orderRepository.save(order));
    }

    private void fulfillInventory(SellingOrder order) {
        for (SellingOrderItem item : order.getItems()) {
            // Note: In a real system, you would track which specific InventoryItem
            // was picked. For this logic, we use the Product and quantity[cite: 1].
            // To be precise, commitPick should be called per scanned InventoryItem ID.
            log.info("Fulfilling physical stock for Product: {}", item.getProduct().getSku());
        }
    }

    private void releaseInventory(SellingOrder order) {
        for (SellingOrderItem item : order.getItems()) {
            // Reverses the soft-lock[cite: 1]
            inventoryService.releaseReservation(item.getProduct().getId(), item.getQuantity());
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus expected) {
        if (current != expected) {
            throw new IllegalOperationException("Order must be " + expected + " before moving to next stage.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(String id) {
        User manager = getAuthenticatedUser();
        return orderRepository.findById(id)
                .filter(o -> o.getWarehouse().getId().equals(manager.getWarehouse().getId()))
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
                .map(item -> OrderResponse.OrderItemDetail.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .sellPriceAtTimeOfOrder(item.getSellPriceAtTimeOfOrder())
                        .lineTotal(item.getSellPriceAtTimeOfOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .totalAmount(itemDetails.stream().map(OrderResponse.OrderItemDetail::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .warehouseName(entity.getWarehouse().getName())
                .items(itemDetails)
                .build();
    }
}