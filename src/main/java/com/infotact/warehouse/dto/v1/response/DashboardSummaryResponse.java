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

    @Schema(description = "Count of products currently below their minimum stock threshold (Procurement)", example = "14")
    private Long lowStockCount;

    /**
     * NEW: Count of products needing movement from Bulk to Picking area.
     */
    @Schema(description = "Count of products needing replenishment in Picking Bins (Operations)", example = "22")
    private Long replenishmentCount;

    @Schema(description = "Number of active outbound customer orders requiring fulfillment", example = "45")
    private Long outboundOrders;

    @Schema(description = "Number of expected inbound shipments from suppliers", example = "8")
    private Long pendingPurchases;

    @Schema(description = "Total number of warehouses managed by the user", example = "1")
    private Long totalWarehouses;

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

    @Schema(description = "Recent inventory transactions acting as an activity feed")
    private List<ActivityDTO> recentActivity;

    /**
     * Profit and revenue summary broken down into thisMonth, lastMonth, and thisYear windows.
     * Each block contains ownedProfit, consignmentProfit, totalProfit, totalRevenue, and binRentalRevenue.
     */
    @Schema(description = "Profit summary for this month, last month, and this year")
    private DashboardProfitSummary profitSummary;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActivityDTO implements Serializable {
        private String message;
        private String timeAgo;
    }

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
