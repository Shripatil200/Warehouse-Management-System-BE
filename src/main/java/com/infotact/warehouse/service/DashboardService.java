package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.response.DashboardSummaryResponse;
import com.infotact.warehouse.dto.v1.response.FinancialMetricResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for high-level business intelligence and operational metrics.
 * <p>
 * This service acts as an aggregator, pulling real-time data from the Inventory,
 * Orders, and Procurement domains to provide a unified 'Snap-shot' of facility
 * health. It is designed to power the primary landing page for authenticated users.
 * </p>
 */
public interface DashboardService {

    /**
     * Generates a comprehensive summary of the current warehouse state.
     * <p>
     * <b>Data Aggregation Logic:</b>
     * 1. <b>Inventory Health:</b> Calculates total stock volume and identifies low-stock alerts.
     * 2. <b>SellingOrder Velocity:</b> Summarizes pending vs. shipped orders for the current period.
     * 3. <b>Facility Utilization:</b> Computes the percentage of occupied storage bins relative to total capacity.
     * 4. <b>Procurement Status:</b> Tracks incoming shipments and identifies overdue purchase orders.
     * </p>
     * <p>
     * <b>Security:</b> This method is strictly multi-tenant; it automatically
     * resolves the current user's Warehouse ID from the security context to
     * ensure data isolation.
     * </p>
     * @return A consolidated {@link DashboardSummaryResponse} containing all key performance indicators (KPIs).
     */
    DashboardSummaryResponse getSummary();

    /**
     * Retrieves dynamic financial performance metrics for the entire warehouse.
     * Used to power historical trend charts (e.g., Revenue vs. Cost) on the main dashboard.
     *
     * @param granularity The time scale for grouping (day, week, month, year)
     * @param start       The start of the reporting period
     * @param end         The end of the reporting period
     * @return A list of financial metrics aggregated by the requested period
     */
    List<FinancialMetricResponse> getWarehouseFinancialPerformance(String granularity, LocalDateTime start, LocalDateTime end);
}