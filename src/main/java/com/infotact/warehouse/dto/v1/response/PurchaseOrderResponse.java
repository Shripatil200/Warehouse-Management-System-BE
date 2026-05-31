package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the state of an inbound stock shipment.
 * <p>
 * Implemented as a regular class rather than a Java record to allow NON_FINAL
 * default typing to record the @class property in the Redis cache, avoiding
 * deserialization conflicts.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed view of an inbound purchase order including financial valuation")
public class PurchaseOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Internal unique identifier (UUID) of the PO",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "Name of the supplier (Denormalized)",
            example = "Apple India Logistics")
    private String supplierName;

    @Schema(description = "Current lifecycle status",
            example = "PLACED")
    private String status;

    @Schema(description = "The display name of the warehouse",
            example = "Pune Fulfillment Center")
    private String warehouseName;

    @Schema(description = "Total financial value of this purchase order",
            example = "3600000.00")
    private BigDecimal totalOrderValue;

    @Schema(description = "The timestamp when the order was issued")
    private LocalDateTime orderDate;

    @Schema(description = "Estimated arrival timestamp")
    private LocalDateTime expectedDate;

    @Schema(description = "List of products, quantities, and costs in this shipment")
    private List<OrderItemDetail> items;

    // Record-style aliases for getter compatibility
    public String id() { return id; }
    public String supplierName() { return supplierName; }
    public String status() { return status; }
    public String warehouseName() { return warehouseName; }
    public BigDecimal totalOrderValue() { return totalOrderValue; }
    public LocalDateTime orderDate() { return orderDate; }
    public LocalDateTime expectedDate() { return expectedDate; }
    public List<OrderItemDetail> items() { return items; }

    /**
     * Detailed view of a single product line item within a purchase order.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Itemized product details with recorded unit cost")
    public static class OrderItemDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "The internal UUID of the product", example = "p-9988-x123")
        private String productId;

        @Schema(description = "The display name of the product", example = "iPhone 17 256GB")
        private String productName;

        @Schema(description = "The SKU used for inventory matching", example = "APP-IP17-256-BLK")
        private String sku;

        @Schema(description = "Quantity ordered", example = "50")
        private Integer quantity;

        @Schema(description = "The purchase price per unit for this specific batch",
                example = "72000.00")
        private BigDecimal unitCost;

        @Schema(description = "Line total (Quantity * Unit Cost)",
                example = "3600000.00")
        private BigDecimal lineTotal;

        // Record-style aliases for getter compatibility
        public String productId() { return productId; }
        public String productName() { return productName; }
        public String sku() { return sku; }
        public Integer quantity() { return quantity; }
        public BigDecimal unitCost() { return unitCost; }
        public BigDecimal lineTotal() { return lineTotal; }
    }
}