package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.*;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import com.infotact.warehouse.repository.BarcodeAuditRepository;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.repository.InventoryTransactionRepository;
import com.infotact.warehouse.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Class-level ensures all methods are read-only and optimized
public class InventoryReportServiceImpl implements InventoryReportService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final BarcodeAuditRepository barcodeAuditRepository;

    @Override
    public Page<InventorySummaryResponse> getGlobalSummary(Pageable pageable) {
        return inventoryRepository.findGlobalInventorySummary(pageable);
    }

    @Override
    public Page<InventoryItemDetailResponse> getDetailedInventory(String sku, String binCode, Pageable pageable) {
        return inventoryRepository.findDetailedInventory(sku, binCode, pageable)
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
                        .availableQuantity(item.getQuantity() - item.getReservedQuantity())
                        .batchNumber(item.getBatchNumber())
                        .expiryDate(item.getExpiryDate())
                        .status(item.getStatus())
                        // Note: Using batch-specific price from the InventoryItem
                        .purchasePrice(item.getPurchasePrice())
                        .build());
    }

    @Override
    public Page<InventoryTransactionResponse> getTransactionHistory(String sku, TransactionType type, Pageable pageable) {
        return transactionRepository.findFilteredTransactions(sku, type, pageable)
                .map(tx -> InventoryTransactionResponse.builder()
                        .transactionId(tx.getId())
                        .timestamp(tx.getTransactionDate()) // Matches your @CreatedDate field
                        .sku(tx.getInventoryItem().getProduct().getSku())
                        .productName(tx.getInventoryItem().getProduct().getName())
                        .type(tx.getType())
                        .quantityChange(tx.getQuantityChange())
                        .reasonCode(tx.getReasonCode())
                        .referenceId(tx.getReferenceId())
                        .binCode(tx.getInventoryItem().getStorageBin().getBinCode())
                        .performedBy(tx.getPerformedBy()) // Matches your @CreatedBy field
                        .unitPrice(tx.getUnitPrice())
                        .build());
    }

    @Override
    public Page<InventorySummaryResponse> getLowStockReport(Integer threshold, Pageable pageable) {
        return inventoryRepository.findLowStockSummary(threshold, pageable);
    }

    @Override
    public Page<BarcodeAuditResponse> getBarcodeAuditLogs(String userId, AuditStatus status, Pageable pageable) {
        return barcodeAuditRepository.findFilteredAudits(userId, status, pageable)
                .map(audit -> BarcodeAuditResponse.builder()
                        .auditId(audit.getId())
                        .timestamp(audit.getTimestamp())
                        .userId(audit.getUserId())
                        .actionType(audit.getActionType())
                        .status(audit.getStatus())
                        .scannedValue(audit.getScannedValue())
                        .errorMessage(audit.getErrorMessage())
                        // You might need a BinRepository join here to get the actual Code
                        .binCode(audit.getBinId())
                        .build());
    }
}