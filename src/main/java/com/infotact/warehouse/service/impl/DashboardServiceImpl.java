package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.AlertDTO;
import com.infotact.warehouse.dto.v1.response.DashboardSummaryResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.OrderStatus;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link DashboardService} for real-time KPI tracking.
 * <p>
 * This service acts as an aggregation hub, merging data from Inventory,
 * Procurement, and Sales domains to provide a facility-level operational snapshot.
 * It enforces multi-tenant isolation via the Security Context.
 * </p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    /**
     * Resolves the identity of the user requesting dashboard data.
     * @return Fully hydrated User entity.
     * @throws UnauthorizedException if no session is found.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No active session found.");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user profile not found."));
    }

    /**
     * Checks if the session holder has administrative or managerial clearance.
     */
    private boolean hasAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") ||
                        a.getAuthority().equalsIgnoreCase("ROLE_MANAGER"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Uses <code>readOnly = true</code> to optimize database performance for high-frequency reads.</li>
     * <li>Orchestrates five independent repository calls into a single response DTO.</li>
     * <li>Calculates physical utilization based on current vs. total capacity metrics.</li>
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
        log.info("Generating real-time operational metrics for Facility: {}", currentUser.getWarehouse().getName());

        return DashboardSummaryResponse.builder()
                .totalProducts(productRepository.countByWarehouseId(warehouseId))
                .lowStockCount(productRepository.countLowStock(warehouseId))
                .outboundOrders(orderRepository.countByWarehouseIdAndStatus(warehouseId, OrderStatus.PENDING))
                .pendingPurchases(purchaseOrderRepository.countByWarehouseIdAndStatus(warehouseId, PurchaseOrderStatus.PENDING))
                .totalWarehouses(warehouseRepository.count())
                .utilizationPercentage(calculateUtilization(warehouseId))
                .alerts(getRecentAlerts(warehouseId))
                .topProducts(orderRepository.findTopSellingProducts(warehouseId, PageRequest.of(0, 3)))
                .build();
    }

    /**
     * Computes the current warehouse fill-rate.
     * <p>
     * <b>Logic:</b> Sums the current occupancy of all stored items and divides it
     * by the total physical capacity defined in the Warehouse metadata.
     * </p>
     */
    private Double calculateUtilization(String warehouseId) {
        // This call must match the method name in the Repository exactly
        Double totalCapacity = warehouseRepository.findTotalCapacityByWarehouseId(warehouseId);

        // Summing occupancy usually comes from the Product/Inventory repository
        Long currentOccupancy = productRepository.sumCurrentOccupancyByWarehouseId(warehouseId);

        if (totalCapacity == null || totalCapacity == 0.0 || currentOccupancy == null) {
            return 0.0;
        }

        return Math.min((currentOccupancy.doubleValue() / totalCapacity) * 100.0, 100.0);
    }

    /**
     * Scans for high-priority operational exceptions.
     * <p>
     * Identifies Low Stock, Overdue Inbound (POs), and Overdue Outbound (Orders).
     * These alerts prioritize immediate action for the Warehouse Manager.
     * </p>
     */
    private List<AlertDTO> getRecentAlerts(String warehouseId) {
        List<AlertDTO> alerts = new ArrayList<>();

        // 1. Low Stock Alerts
        productRepository.findLowStockProducts(warehouseId).stream()
                .limit(2)
                .forEach(p -> alerts.add(new AlertDTO(p.getName() + " - Low Stock", "LOW_STOCK")));

        // 2. Delayed Outbound Alerts
        long delayedOrders = orderRepository.countDelayedOrders(warehouseId);
        if (delayedOrders > 0) {
            alerts.add(new AlertDTO(delayedOrders + " Sales Orders are Overdue", "DELAYED_ORDER"));
        }

        // 3. Delayed Inbound Alerts
        long delayedPOs = purchaseOrderRepository.countDelayedPurchaseOrders(warehouseId, PurchaseOrderStatus.RECEIVED);
        if (delayedPOs > 0) {
            alerts.add(new AlertDTO(delayedPOs + " Inbound Shipments are Overdue", "DELAYED_PO"));
        }

        return alerts;
    }
}