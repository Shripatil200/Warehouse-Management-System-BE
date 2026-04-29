package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the state of an inbound stock shipment.
 * <p>
 * Provides a comprehensive view of stock expected from a supplier, including
 * the financial value of the shipment for inventory valuation.
 * </p>
 */
@Schema(description = "Detailed view of an inbound purchase order including financial valuation")
public record PurchaseOrderResponse (
        @Schema(description = "Internal unique identifier (UUID) of the PO",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Name of the supplier (Denormalized)",
                example = "Apple India Logistics")
        String supplierName,

        @Schema(description = "Current lifecycle status",
                example = "PLACED")
        String status,

        @Schema(description = "The display name of the warehouse",
                example = "Pune Fulfillment Center")
        String warehouseName,

        @Schema(description = "Total financial value of this purchase order",
                example = "3600000.00")
        BigDecimal totalOrderValue,

        @Schema(description = "The timestamp when the order was issued")
        LocalDateTime orderDate,

        @Schema(description = "Estimated arrival timestamp")
        LocalDateTime expectedDate,

        @Schema(description = "List of products, quantities, and costs in this shipment")
        List<OrderItemDetail> items
) {
    /**
     * Detailed view of a single product line item within a purchase order.
     */
    @Schema(description = "Itemized product details with recorded unit cost")
    public record OrderItemDetail(
            @Schema(description = "The internal UUID of the product", example = "p-9988-x123")
            String productId,

            @Schema(description = "The display name of the product", example = "iPhone 17 256GB")
            String productName,

            @Schema(description = "The SKU used for inventory matching", example = "APP-IP17-256-BLK")
            String sku,

            @Schema(description = "Quantity ordered", example = "50")
            Integer quantity,

            @Schema(description = "The purchase price per unit for this specific batch",
                    example = "72000.00")
            BigDecimal unitCost,

            @Schema(description = "Line total (Quantity * Unit Cost)",
                    example = "3600000.00")
            BigDecimal lineTotal
    ) {}
}