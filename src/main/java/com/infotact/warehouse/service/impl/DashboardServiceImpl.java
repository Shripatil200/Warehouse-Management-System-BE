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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        log.info("Generating real-time metrics for Facility: {} (ID: {})",
                currentUser.getWarehouse().getName(), warehouseId);

        // This now counts unique warehouses linked to the user's email
        long warehouseCount = userRepository.countAssignedWarehouseForUser(currentUser.getEmail());

        return DashboardSummaryResponse.builder()
                .totalProducts(productRepository.countByWarehouseId(warehouseId))
                .lowStockCount(productRepository.countLowStock(warehouseId))
                .outboundOrders(orderRepository.countByWarehouseIdAndStatus(warehouseId, OrderStatus.PENDING))
                .pendingPurchases(purchaseOrderRepository.countByWarehouseIdAndStatus(warehouseId, PurchaseOrderStatus.PENDING))
                .totalWarehouses(warehouseCount)
                .utilizationPercentage(calculateUtilization(warehouseId))
                .alerts(getRecentAlerts(warehouseId))
                .topProducts(orderRepository.findTopSellingProducts(warehouseId, PageRequest.of(0, 3)))
                .build();
    }

    private Double calculateUtilization(String warehouseId) {
        // Handle nulls safely for new warehouses
        Double totalCapacity = warehouseRepository.findTotalCapacityByWarehouseId(warehouseId);
        Long currentOccupancy = productRepository.sumCurrentOccupancyByWarehouseId(warehouseId);

        // Normalize values to avoid NPE and Division by Zero
        double capacity = Optional.ofNullable(totalCapacity).orElse(0.0);
        long occupancy = Optional.ofNullable(currentOccupancy).orElse(0L);

        if (capacity <= 0.0) {
            log.warn("Capacity not defined or zero for warehouse: {}", warehouseId);
            return 0.0;
        }

        double percentage = (occupancy / capacity) * 100.0;

        // Ensure we don't return infinity or more than 100% unless business logic allows
        return Math.min(Math.max(0.0, percentage), 100.0);
    }

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

        // 3. Delayed Inbound Alerts (Check for status received/pending based on logic)
        long delayedPOs = purchaseOrderRepository.countDelayedPurchaseOrders(warehouseId, PurchaseOrderStatus.PENDING);
        if (delayedPOs > 0) {
            alerts.add(new AlertDTO(delayedPOs + " Inbound Shipments are Overdue", "DELAYED_PO"));
        }

        return alerts;
    }
}