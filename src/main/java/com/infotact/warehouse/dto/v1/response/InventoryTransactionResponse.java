package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionResponse {
    private String transactionId;
    private LocalDateTime timestamp; // Maps to transactionDate
    private String sku;
    private String productName;
    private TransactionType type;
    private Long quantityChange;
    private String reasonCode;
    private String referenceId;
    private String binCode;
    private String performedBy;
    private BigDecimal unitPrice;
}