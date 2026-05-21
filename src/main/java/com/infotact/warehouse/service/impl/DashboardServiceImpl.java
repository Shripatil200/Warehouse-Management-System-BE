package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.*;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Service implementation for high-performance dashboard analytics.
 *
 * Performance Features:
 * - Dashboard Caching: Stores summaries in RAM via @Cacheable.
 * - Aggregate SQL: Refined to use Parameterized Enums for Type Safety.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardSummariesV3", key = "#root.target.getAuthenticatedUserWarehouseId()")
    public DashboardSummaryResponse getSummary() {
        User currentUser = getAuthenticatedUser();
        String warehouseId = currentUser.getWarehouse().getId();

        log.info("Generating analytics for facility: {}", currentUser.getWarehouse().getName());

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return DashboardSummaryResponse.builder()
                .totalProducts(productRepository.countByWarehouseId(warehouseId))
                .lowStockCount(productRepository.countGlobalLowStock(warehouseId))
                // Refactored: Now passing BinType.PICK_FACE as a parameter
                .replenishmentCount(productRepository.countProductsNeedingReplenishment(warehouseId, BinType.PICK_FACE))
                .outboundOrders(orderRepository.countByWarehouseIdAndStatus(warehouseId, OrderStatus.PENDING))
                .pendingPurchases(purchaseOrderRepository.countByWarehouseIdAndStatus(warehouseId, PurchaseOrderStatus.PENDING))
                .totalWarehouses(userRepository.countAssignedWarehouseForUser(currentUser.getEmail()))
                .userStatusCounts(fetchUserStatusCounts(warehouseId))
                .utilizationPercentage(calculateUtilization(warehouseId))
                .totalInventoryValue(calculateStockValue(warehouseId))
                .alerts(getRecentAlerts(warehouseId))
                .recentActivity(fetchRecentActivity(warehouseId))
                .topProducts(orderRepository.findTopSellingProductsMonthly(warehouseId, thirtyDaysAgo, PageRequest.of(0, 3)))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialMetricResponse> getWarehouseFinancialPerformance(String granularity, LocalDateTime start, LocalDateTime end) {
        User currentUser = getAuthenticatedUser();
        String warehouseId = currentUser.getWarehouse().getId();
        String format = resolveFormat(granularity);

        return transactionRepository.getFinancialAnalytics(warehouseId, null, start, end, format);
    }

    /**
     * Internal helper to resolve warehouse occupancy/utilization.
     */
    private Double calculateUtilization(String warehouseId) {
        List<Object[]> results = warehouseRepository.findWarehouseUtilization(warehouseId);

        if (results != null && !results.isEmpty()) {
            Object[] row = results.get(0);
            Double capacity = (row[0] != null) ? ((Number) row[0]).doubleValue() : 0.0;
            Double occupancy = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;

            if (capacity <= 0.0) {
                log.warn("Utilization check: Warehouse {} has no defined bin capacity.", warehouseId);
                return 0.0;
            }

            double percentage = (occupancy / capacity) * 100.0;
            return Math.min(100.0, percentage);
        }

        return 0.0;
    }

    private BigDecimal calculateStockValue(String warehouseId) {
        return Optional.ofNullable(inventoryItemRepository.calculateTotalValueByWarehouse(warehouseId))
                .orElse(BigDecimal.ZERO);
    }

    private DashboardSummaryResponse.UserStatusCountDTO fetchUserStatusCounts(String warehouseId) {
        List<Object[]> results = userRepository.countUsersByStatus(warehouseId);
        long active = 0, inactive = 0, suspended = 0, pending = 0;

        for (Object[] result : results) {
            if (result[0] == null) continue;
            String status = result[0].toString();
            long count = ((Number) result[1]).longValue();

            switch (status) {
                case "ACTIVE" -> active = count;
                case "INACTIVE" -> inactive = count;
                case "SUSPENDED" -> suspended = count;
                case "PENDING" -> pending = count;
            }
        }
        return new DashboardSummaryResponse.UserStatusCountDTO(active, inactive, suspended, pending);
    }

    /**
     * Generates a list of high-priority operational alerts.
     */
    private List<AlertDTO> getRecentAlerts(String warehouseId) {
        List<AlertDTO> alerts = new ArrayList<>();

        // 1. Procurement Alerts: Items missing from the entire warehouse
        productRepository.findGlobalLowStockProducts(warehouseId).stream()
                .limit(2)
                .forEach(p -> alerts.add(new AlertDTO(p.getName() + " - Low Global Stock", "LOW_STOCK")));

        // 2. Operational Alerts: Items available in Bulk but missing from Picking shelf
        long replenishNeeded = productRepository.countProductsNeedingReplenishment(warehouseId, BinType.PICK_FACE);
        if (replenishNeeded > 0) {
            alerts.add(new AlertDTO(replenishNeeded + " Products require Picking Replenishment", "REPLENISHMENT"));
        }

        // 3. Logistics Alerts: Delayed outbound orders
        List<OrderStatus> excluded = List.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        long delayed = orderRepository.countDelayedOrders(warehouseId, excluded);

        if (delayed > 0) {
            alerts.add(new AlertDTO(delayed + " Orders Overdue", "DELAYED"));
        }

        return alerts;
    }

    private List<DashboardSummaryResponse.ActivityDTO> fetchRecentActivity(String warehouseId) {
        return transactionRepository.findRecentActivityByWarehouseId(warehouseId, PageRequest.of(0, 3))
                .stream()
                .map(t -> DashboardSummaryResponse.ActivityDTO.builder()
                        .message(buildActivityMessage(t))
                        .timeAgo(calculateTimeAgo(t.getTransactionDate()))
                        .build())
                .toList();
    }

    private String buildActivityMessage(com.infotact.warehouse.entity.InventoryTransaction t) {
        String name = t.getInventoryItem().getProduct().getName();
        return switch (t.getType()) {
            case INBOUND -> "Received " + t.getQuantityChange() + " units of " + name;
            case OUTBOUND -> "Dispatched " + Math.abs(t.getQuantityChange()) + " units of " + name;
            case TRANSFER -> "Transferred " + Math.abs(t.getQuantityChange()) + " units of " + name;
            case ADJUSTMENT -> "Adjusted " + name + " stock by " + t.getQuantityChange();
            default -> "Inventory updated for " + name;
        };
    }

    private String calculateTimeAgo(LocalDateTime past) {
        if (past == null) return "Just now";
        Duration duration = Duration.between(past, LocalDateTime.now());
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() <= 1 ? "Just now" : duration.toMinutes() + " minutes ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + " hours ago";
        } else {
            return duration.toDays() + " days ago";
        }
    }

    public String getAuthenticatedUserWarehouseId() {
        return getAuthenticatedUser().getWarehouse().getId();
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Session expired or user not authenticated.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("User profile not found."));
    }

    private String resolveFormat(String granularity) {
        return switch (granularity.toLowerCase()) {
            case "day" -> "%Y-%m-%d";
            case "week" -> "%Y-Week%u";
            case "month" -> "%Y-%m";
            case "year" -> "%Y";
            default -> "%Y-%m-%d";
        };
    }
}