package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.response.*;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
     * Service for handling complex inventory queries and management reports.
     * This service is read-only and optimized for high-performance data retrieval.
     */
    public interface InventoryReportService {

        /**
         * Retrieves an aggregated summary of all inventory, grouped by Product/SKU.
         * Includes financial valuation based on current purchase prices.
         *
         * @param pageable Pagination and sorting information
         * @return A page of inventory summaries
         */
        Page<InventorySummaryResponse> getGlobalSummary(Pageable pageable);

        /**
         * Retrieves granular details for specific inventory items (stock slices).
         * Used for auditing specific bins or batch numbers.
         *
         * @param sku Optional filter by Product SKU
         * @param binCode Optional filter by Storage Bin barcode
         * @param pageable Pagination and sorting information
         * @return A page of detailed inventory items
         */
        Page<InventoryItemDetailResponse> getDetailedInventory(String sku, String binCode, Pageable pageable);

        /**
         * Retrieves a chronological log of all inventory transactions.
         * Used as the primary audit trail for stock movements.
         *
         * @param sku Optional filter by Product SKU
         * @param type Optional filter by Transaction Type (e.g., ADJUSTMENT, PICK)
         * @param pageable Pagination and sorting information
         * @return A page of transaction log entries
         */
        Page<InventoryTransactionResponse> getTransactionHistory(String sku, TransactionType type, Pageable pageable);

        /**
         * Generates a report of items where total available quantity is below the threshold.
         * Useful for purchasing managers to trigger restock orders.
         *
         * @param threshold The quantity limit to be considered "low stock"
         * @param pageable Pagination and sorting information
         * @return A page of summaries for items requiring attention
         */
        Page<InventorySummaryResponse> getLowStockReport(Integer threshold, Pageable pageable);

        /**
         * Retrieves a paginated history of physical barcode scanning events.
         * Used by management to monitor operator accuracy, identify scanning bottlenecks,
         * and verify that physical interactions at the bin level align with system records.
         *
         * @param userId Optional filter to view the scanning performance of a specific operator
         * @param status Optional filter to isolate SUCCESS or FAILURE events (e.g., wrong bin scans)
         * @param pageable Pagination and sorting information, typically sorted by timestamp descending
         * @return A page of barcode audit responses providing visibility into floor behavior
         */
        Page<BarcodeAuditResponse> getBarcodeAuditLogs(String userId, AuditStatus status, Pageable pageable);

        /**
         * Retrieves dynamic financial performance metrics for a specific product.
         * Enables deep-dive analysis into product-specific margins, inbound costs,
         * and adjustment losses over time.
         *
         * @param productId   The unique identifier of the product to analyze
         * @param granularity The time scale for grouping (day, week, month, year)
         * @param start       The start of the reporting period
         * @param end         The end of the reporting period
         * @return A list of financial metrics specific to the product, aggregated by period
         */
        List<FinancialMetricResponse> getProductFinancialPerformance(String productId, String granularity, LocalDateTime start, LocalDateTime end);
}
