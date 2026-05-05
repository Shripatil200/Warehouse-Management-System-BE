package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.response.BarcodeAuditResponse;
import com.infotact.warehouse.dto.v1.response.InventoryItemDetailResponse;
import com.infotact.warehouse.dto.v1.response.InventorySummaryResponse;
import com.infotact.warehouse.dto.v1.response.InventoryTransactionResponse;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

}
