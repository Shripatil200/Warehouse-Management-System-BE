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
 * Fixed to align with the Warehouse-Isolated Product Repository (v2.3).
 * All SKU lookups now require the authenticated manager's Warehouse ID.
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
     * <b>Update:</b> SKU resolution is now scoped to the manager's warehouse to
     * prevent cross-facility order injection.
     * </p>
     */
    @Override
    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public OrderResponse createOrder(OrderRequest request) {
        User manager = getAuthenticatedUser();
        String warehouseId = manager.getWarehouse().getId();

        log.info("Creating Order {} for Facility: {}", request.getOrderNumber(), manager.getWarehouse().getName());

        SellingOrder order = new SellingOrder();
        order.setOrderNumber(request.getOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setWarehouse(manager.getWarehouse());
        order.setCreatedAt(LocalDateTime.now());
        order.setExpectedShipDate(LocalDateTime.now().plusDays(2));

        List<SellingOrderItem> items = request.getItems().stream().map(itemReq -> {
            // FIX: Use the new warehouse-scoped repository method
            Product product = productRepository.findBySkuAndWarehouseIdAndActiveTrue(itemReq.getSku(), warehouseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product SKU '" + itemReq.getSku() + "' not found in your warehouse."));

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
     * {@inheritDoc}
     * <p>
     * <b>Security:</b> Validates that the requested order belongs to the
     * manager's warehouse context.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(String id) {
        User manager = getAuthenticatedUser();

        // We assume OrderRepository also has a warehouse-aware find method
        return orderRepository.findById(id)
                .filter(o -> o.getWarehouse().getId().equals(manager.getWarehouse().getId()))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied."));
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