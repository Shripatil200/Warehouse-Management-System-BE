package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.AlertDTO;
import com.infotact.warehouse.dto.v1.response.DashboardSummaryResponse;
import com.infotact.warehouse.dto.v1.response.ProductSalesDTO;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link DashboardService} for real-time facility analytics.
 * <p>
 * This service aggregates high-level metrics across products, orders, and users.
 * It enforces facility-scoped security by filtering data based on the authenticated
 * manager's assigned warehouse.
 * </p>
 * <p>
 * <b>Update:</b> Now incorporates <b>Financial Valuation</b> and <b>Monthly Analytics</b>.
 * The dashboard calculates stock value using batch-specific prices (10rs vs 12rs)
 * and identifies top-selling products based on the last 30 days of activity.
 * </p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Resolves the current manager's profile to maintain facility-scoped data integrity.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No active session found.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    private boolean hasAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") ||
                        a.getAuthority().equalsIgnoreCase("ROLE_MANAGER"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Metrics Generation:</b>
     * <ul>
     * <li><b>Financial:</b> Calculates total value of on-hand inventory via {@link InventoryItemRepository}.</li>
     * <li><b>Operational:</b> Tracks pending inbound/outbound queues and utilization.</li>
     * <li><b>Analytics:</b> Fetches top products specifically for the <b>current month</b>.</li>
     * </ul>
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        if (!hasAccess()) {
            throw new UnauthorizedException("Access restricted to Admins and Managers.");
        }

        User currentUser = getAuthenticatedUser();
        if (currentUser.getWarehouse() == null) {
            throw new UnauthorizedException("User profile is not associated with a facility.");
        }

        String warehouseId = currentUser.getWarehouse().getId();
        log.info("Generating real-time financial and operational metrics for: {}",
                currentUser.getWarehouse().getName());

        // Calculate time window for monthly trends
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 1. Existing Metrics
        long warehouseCount = userRepository.countAssignedWarehouseForUser(currentUser.getEmail());
        DashboardSummaryResponse.UserStatusCountDTO statusCounts = fetchUserStatusCounts();

        // 2. Financial Metrics (10rs vs 12rs logic)
        BigDecimal totalStockValue = calculateStockValue(warehouseId);

        // 3. Analytics: Fetch only products that performed well in the last 30 days
        List<ProductSalesDTO> topPerformingProducts = orderRepository.findTopSellingProductsMonthly(
                warehouseId,
                thirtyDaysAgo,
                PageRequest.of(0, 3)
        );

        return DashboardSummaryResponse.builder()
                .totalProducts(productRepository.countByWarehouseId(warehouseId))
                .lowStockCount(productRepository.countLowStock(warehouseId))
                .outboundOrders(orderRepository.countByWarehouseIdAndStatus(warehouseId, OrderStatus.PENDING))
                .pendingPurchases(purchaseOrderRepository.countByWarehouseIdAndStatus(warehouseId, PurchaseOrderStatus.PENDING))
                .totalWarehouses(warehouseCount)
                .userStatusCounts(statusCounts)
                .utilizationPercentage(calculateUtilization(warehouseId))
                .totalInventoryValue(totalStockValue)
                .alerts(getRecentAlerts(warehouseId))
                .topProducts(topPerformingProducts) // Monthly Trends
                .build();
    }

    /**
     * Calculates the total financial value of all stock in the warehouse.
     * Logic: Sums (quantity * purchasePrice) across all batches in the warehouse.
     */
    private BigDecimal calculateStockValue(String warehouseId) {
        return Optional.ofNullable(inventoryItemRepository.calculateTotalValueByWarehouse(warehouseId))
                .orElse(BigDecimal.ZERO);
    }

    private DashboardSummaryResponse.UserStatusCountDTO fetchUserStatusCounts() {
        List<Object[]> results = userRepository.countUsersByStatus();
        long active = 0, inactive = 0, suspended = 0, pending = 0;

        for (Object[] result : results) {
            String statusStr = result[0].toString();
            long count = (long) result[1];

            switch (statusStr) {
                case "ACTIVE" -> active = count;
                case "INACTIVE" -> inactive = count;
                case "SUSPENDED" -> suspended = count;
                case "PENDING" -> pending = count;
            }
        }

        return DashboardSummaryResponse.UserStatusCountDTO.builder()
                .active(active)
                .inactive(inactive)
                .suspended(suspended)
                .pending(pending)
                .build();
    }

    private Double calculateUtilization(String warehouseId) {
        Double totalCapacity = warehouseRepository.findTotalCapacityByWarehouseId(warehouseId);
        double currentOccupancy = productRepository.sumCurrentOccupancyByWarehouseId(warehouseId);

        double capacity = Optional.ofNullable(totalCapacity).orElse(0.0);
        double occupancy = Optional.ofNullable(currentOccupancy).orElse(0.0);

        if (capacity <= 0.0) return 0.0;
        return Math.min(Math.max(0.0, (occupancy / capacity) * 100.0), 100.0);
    }

    private List<AlertDTO> getRecentAlerts(String warehouseId) {
        List<AlertDTO> alerts = new ArrayList<>();

        productRepository.findLowStockProducts(warehouseId).stream()
                .limit(2)
                .forEach(p -> alerts.add(new AlertDTO(p.getName() + " - Low Stock Alert", "LOW_STOCK")));

        long delayedOrders = orderRepository.countDelayedOrders(warehouseId);
        if (delayedOrders > 0) {
            alerts.add(new AlertDTO(delayedOrders + " Sales Orders are Overdue", "DELAYED_ORDER"));
        }

        return alerts;
    }
}