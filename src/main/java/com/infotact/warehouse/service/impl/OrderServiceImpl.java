package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.OrderRequest;
import com.infotact.warehouse.dto.v1.response.OrderResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.OrderRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.repository.UserRepository;
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

/**
 * Implementation of {@link OrderService} managing the outbound fulfillment cycle.
 * <p>
 * This service handles the transition of customer demand into actionable warehouse tasks.
 * It enforces multi-tenant data isolation by anchoring every order to the manager's
 * assigned warehouse context.
 * </p>
 * <p>
 * <b>Update:</b> Now incorporates <b>Financial Snapshotting</b> logic, capturing
 * the product selling price at the moment of order placement to preserve
 * historical revenue data.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Resolves the current manager's profile to enforce warehouse-level security.
     */
    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated profile not found"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>SKU Resolution:</b> Translates SKU strings into Product entities.</li>
     * <li><b>Price Snapshot:</b> Captures the current Product sellingPrice for the order item.</li>
     * <li><b>SLA Management:</b> Sets <code>expectedShipDate</code> (current + 48hrs).</li>
     * </ul>
     */
    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(OrderRequest request) {
        User manager = getAuthenticatedUser();
        log.info("Processing outbound order {} for Warehouse: {}", request.getOrderNumber(), manager.getWarehouse().getName());

        SellingOrder order = new SellingOrder();
        order.setOrderNumber(request.getOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setWarehouse(manager.getWarehouse());
        order.setCreatedAt(LocalDateTime.now());
        order.setExpectedShipDate(LocalDateTime.now().plusDays(2));

        List<SellingOrderItem> items = request.getItems().stream().map(itemReq -> {
            Product product = productRepository.findBySkuAndActiveTrue(itemReq.getSku())
                    .orElseThrow(() -> new ResourceNotFoundException("Active product not found: " + itemReq.getSku()));

            SellingOrderItem item = new SellingOrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());

            // Financial Snapshot: Capture the selling price at this exact moment
            item.setSellPriceAtTimeOfOrder(product.getSellingPrice());

            return item;
        }).toList();

        order.setItems(items);
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(String id) {
        return orderRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "'list-' + #status")
    public List<OrderResponse> getWarehouseOrders(String status) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        List<SellingOrder> orders;
        if (status != null && !status.isBlank()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            orders = orderRepository.findAllByWarehouseIdAndStatus(warehouseId, orderStatus);
        } else {
            orders = orderRepository.findAllByWarehouseId(warehouseId);
        }

        return orders.stream().map(this::mapToResponse).toList();
    }

    /**
     * Maps the SellingOrder entity to a response DTO with financial calculations.
     */
    private OrderResponse mapToResponse(SellingOrder entity) {
        List<OrderResponse.OrderItemDetail> itemDetails = entity.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getSellPriceAtTimeOfOrder()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    return OrderResponse.OrderItemDetail.builder()
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName())
                            .sku(item.getProduct().getSku())
                            .quantity(item.getQuantity())
                            .sellPriceAtTimeOfOrder(item.getSellPriceAtTimeOfOrder())
                            .lineTotal(lineTotal)
                            .build();
                })
                .toList();

        // Calculate total order value
        BigDecimal totalAmount = itemDetails.stream()
                .map(OrderResponse.OrderItemDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponse.builder()
                .id(entity.getId())
                .orderNumber(entity.getOrderNumber())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .totalAmount(totalAmount)
                .warehouseName(entity.getWarehouse().getName())
                .items(itemDetails)
                .build();
    }
}