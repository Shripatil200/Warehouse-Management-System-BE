package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.dto.v1.response.*;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import com.infotact.warehouse.repository.BarcodeAuditRepository;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.repository.InventoryTransactionRepository;
import com.infotact.warehouse.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportServiceImpl implements InventoryReportService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final BarcodeAuditRepository barcodeAuditRepository;
    private final WarehouseContext warehouseContext;

    // ============================================================
    // GLOBAL SUMMARY
    // ============================================================

    @Override
    public Page<InventorySummaryResponse> getGlobalSummary(Pageable pageable) {

        String warehouseId = warehouseContext.getWarehouseId();

        return inventoryRepository.findGlobalInventorySummaryByWarehouse(
                warehouseId,
                pageable
        );
    }

    // ============================================================
    // DETAILED INVENTORY
    // ============================================================

    @Override
    public Page<InventoryItemDetailResponse> getDetailedInventory(
            String sku,
            String binCode,
            Pageable pageable) {

        String warehouseId = warehouseContext.getWarehouseId();

        return inventoryRepository.findDetailedInventory(
                        sku,
                        binCode,
                        warehouseId,
                        pageable
                )
                .map(item -> InventoryItemDetailResponse.builder()
                        .inventoryItemId(item.getId())
                        .productId(item.getProduct().getId())
                        .sku(item.getProduct().getSku())
                        .productName(item.getProduct().getName())
                        .binId(item.getStorageBin().getId())
                        .binCode(item.getStorageBin().getBinCode())
                        .binType(item.getStorageBin().getBinType())
                        .physicalQuantity(item.getQuantity())
                        .reservedQuantity(item.getReservedQuantity())
                        .availableQuantity(item.getAvailableQuantity())
                        .batchNumber(item.getBatchNumber())
                        .expiryDate(item.getExpiryDate())
                        .status(item.getStatus())
                        .purchasePrice(item.getPurchasePrice())
                        .build());
    }

    // ============================================================
    // TRANSACTION HISTORY
    // ============================================================

    @Override
    public Page<InventoryTransactionResponse> getTransactionHistory(
            String sku,
            TransactionType type,
            Pageable pageable) {

        String warehouseId = warehouseContext.getWarehouseId();

        return transactionRepository.findFilteredTransactions(
                        sku,
                        type,
                        warehouseId,
                        pageable
                )
                .map(tx -> InventoryTransactionResponse.builder()
                        .transactionId(tx.getId())
                        .timestamp(tx.getTransactionDate())
                        .sku(tx.getInventoryItem().getProduct().getSku())
                        .productName(tx.getInventoryItem().getProduct().getName())
                        .type(tx.getType())
                        .quantityChange(tx.getQuantityChange())
                        .reasonCode(tx.getReasonCode())
                        .referenceId(tx.getReferenceId())
                        .binCode(tx.getInventoryItem().getStorageBin().getBinCode())
                        .performedBy(tx.getPerformedBy())
                        .unitPrice(tx.getUnitPrice())
                        .build());
    }

    // ============================================================
    // LOW STOCK
    // ============================================================

    @Override
    public Page<InventorySummaryResponse> getLowStockReport(
            Integer threshold,
            Pageable pageable) {

        String warehouseId = warehouseContext.getWarehouseId();

        return inventoryRepository.findLowStockSummaryByWarehouse(
                threshold,
                warehouseId,
                pageable
        );
    }

    // ============================================================
    // BARCODE AUDIT LOGS (SECURE)
    // ============================================================

    @Override
    public Page<BarcodeAuditResponse> getBarcodeAuditLogs(
            String userId,
            AuditStatus status,
            Pageable pageable) {

        String warehouseId = warehouseContext.getWarehouseId();

        return barcodeAuditRepository.findFilteredAudits(
                        userId,
                        status,
                        warehouseId,
                        pageable
                )
                .map(audit -> BarcodeAuditResponse.builder()
                        .auditId(audit.getId())
                        .timestamp(audit.getTimestamp())
                        .userId(audit.getUserId())
                        .actionType(audit.getActionType())
                        .status(audit.getStatus())
                        .scannedValue(audit.getScannedValue())
                        .errorMessage(audit.getErrorMessage())
                        .binCode(audit.getBinId())
                        .build());
    }

    // ============================================================
    // FINANCIAL ANALYTICS
    // ============================================================

    @Override
    public List<FinancialMetricResponse> getProductFinancialPerformance(
            String productId,
            String granularity,
            LocalDateTime start,
            LocalDateTime end) {

        String warehouseId = warehouseContext.getWarehouseId();

        String format = resolveFormat(granularity);

        return transactionRepository.getFinancialAnalytics(
                warehouseId,
                productId,
                start,
                end,
                format
        );
    }

    // ============================================================
    // HELPER
    // ============================================================

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