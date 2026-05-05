package com.infotact.warehouse.controller.v1;

import com.infotact.warehouse.dto.v1.response.*;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import com.infotact.warehouse.service.InventoryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory-reports")
@RequiredArgsConstructor
@Tag(name = "Inventory Reporting", description = "Restricted management-level reports for stock valuation and audit trails.")
// Apply security at the class level: Only Managers and Admins can enter
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    @Operation(
            summary = "Get Global Inventory Summary",
            description = "Aggregated stock levels per SKU with financial valuation. Access: ADMIN, MANAGER."
    )
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    @GetMapping("/global-summary")
    public ResponseEntity<Page<InventorySummaryResponse>> getGlobalSummary(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(inventoryReportService.getGlobalSummary(pageable));
    }

    @Operation(
            summary = "Get Granular Stock Details",
            description = "Detailed view of every inventory 'slice' (bins, batches, expiries). Access: ADMIN, MANAGER."
    )
    @GetMapping("/stock-details")
    public ResponseEntity<Page<InventoryItemDetailResponse>> getStockDetails(
            @Parameter(description = "Filter by SKU") @RequestParam(required = false) String sku,
            @Parameter(description = "Filter by Bin Code") @RequestParam(required = false) String binCode,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(inventoryReportService.getDetailedInventory(sku, binCode, pageable));
    }

    @Operation(
            summary = "Get Transaction History (Audit Logs)",
            description = "Chronological log of all stock movements. Access: ADMIN, MANAGER."
    )
    @GetMapping("/transactions")
    public ResponseEntity<Page<InventoryTransactionResponse>> getTransactionHistory(
            @Parameter(description = "Filter by SKU") @RequestParam(required = false) String sku,
            @Parameter(description = "Filter by Action Type") @RequestParam(required = false) TransactionType type,
            // Updated sort to match entity field: transactionDate
            @PageableDefault(size = 50, sort = "transactionDate") Pageable pageable) {
        return ResponseEntity.ok(inventoryReportService.getTransactionHistory(sku, type, pageable));
    }

    @Operation(
            summary = "Low Stock Alert Report",
            description = "Identifies products below a specified threshold for reordering. Access: ADMIN, MANAGER."
    )
    @GetMapping("/low-stock")
    public ResponseEntity<Page<InventorySummaryResponse>> getLowStockReport(
            @Parameter(description = "The minimum available quantity threshold") @RequestParam(defaultValue = "10") Integer threshold,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(inventoryReportService.getLowStockReport(threshold, pageable));
    }

    @Operation(
            summary = "Get Barcode Scan Audit Logs",
            description = "Audit physical scanning behavior (Success/Failure). Access: ADMIN, MANAGER."
    )
    @GetMapping("/barcode-audits")
    public ResponseEntity<Page<BarcodeAuditResponse>> getBarcodeAudits(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditStatus status,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryReportService.getBarcodeAuditLogs(userId, status, pageable));
    }

    @GetMapping("/products/{productId}/finance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<FinancialMetricResponse>> getProductFinance(
            @PathVariable String productId,
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        return ResponseEntity.ok(inventoryReportService.getProductFinancialPerformance(productId, granularity, start, end));
    }
}