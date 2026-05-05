package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InventoryItemDetailResponse {
    private String inventoryItemId;

    // Add these missing fields
    private String productId;
    private String binId;

    private String sku;
    private String productName;
    private String binCode;
    private BinType binType;

    private Integer physicalQuantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;

    private String batchNumber;
    private LocalDate expiryDate;
    private InventoryStatus status;
    private BigDecimal purchasePrice;
}