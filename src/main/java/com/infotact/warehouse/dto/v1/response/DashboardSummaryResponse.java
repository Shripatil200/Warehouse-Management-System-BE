package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated response object for the Management Dashboard.
 * <p>
 * This DTO provides an aggregated view of the warehouse's current operational state.
 * It combines real-time counts, performance metrics (utilization), and prioritized
 * lists for alerts and sales performance.
 * </p>
 * <p>
 * <b>Update:</b> Now includes <b>totalInventoryValue</b> to provide financial
 * visibility into the warehouse's current stock valuation based on batch costs.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(
        name = "DashboardSummaryResponse",
        description = "Consolidated operational metrics and analytics for the warehouse dashboard"
)
public class DashboardSummaryResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Total number of unique products in the catalog", example = "1250")
    private Long totalProducts;

    @Schema(description = "Count of products currently below their minimum stock threshold", example = "14")
    private Long lowStockCount;

    @Schema(description = "Number of active outbound customer orders requiring fulfillment", example = "45")
    private Long outboundOrders;

    @Schema(description = "Number of expected inbound shipments from suppliers", example = "8")
    private Long pendingPurchases;

    @Schema(description = "Total number of warehouses managed by the user", example = "1")
    private Long totalWarehouses;

    /**
     * Total monetary value of all physical stock currently in the warehouse.
     * <p>
     * Logic: Calculated as the sum of (InventoryItem.quantity * InventoryItem.purchasePrice).
     * This accurately reflects batch-level costs (e.g., items bought at 10rs vs 12rs).
     * </p>
     */
    @Schema(description = "Total financial valuation of on-hand inventory", example = "2540500.00")
    private BigDecimal totalInventoryValue;

    @Schema(description = "Current warehouse storage occupancy level as a percentage", example = "78.5")
    private Double utilizationPercentage;

    @Schema(description = "List of high-priority operational alerts and notifications")
    private List<AlertDTO> alerts;

    @Schema(description = "Ranking of the highest-performing products by sales volume and profit")
    private List<ProductSalesDTO> topProducts;

    @Schema(description = "Count of users categorized by their account status")
    private UserStatusCountDTO userStatusCounts;

    /**
     * Inner DTO representing user lifecycle distribution.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserStatusCountDTO {
        private Long active;
        private Long inactive;
        private Long suspended;
        private Long pending;
    }
}