package com.infotact.warehouse.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the state of an inbound stock shipment.
 * <p>
 * This response provides a comprehensive view of stock expected from a supplier.
 * It includes denormalized facility names and detailed line items to facilitate
 * the receiving and verification process at the warehouse dock.
 * </p>
 */
@Schema(description = "Detailed view of an inbound purchase order and its fulfillment status")
public record PurchaseOrderResponse(
        @Schema(description = "Internal unique identifier (UUID) of the PO",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Name of the supplier providing the goods (Denormalized)",
                example = "Global Tech Supplies")
        String supplierName,

        @Schema(description = "Current lifecycle status of the PO",
                example = "PLACED", allowableValues = {"PLACED", "SHIPPED", "PARTIAL", "RECEIVED", "CANCELLED"})
        String status,

        @Schema(description = "The UUID of the destination warehouse",
                example = "wh-a1b2-c3d4")
        String warehouseId,

        @Schema(description = "The display name of the destination warehouse (Denormalized)",
                example = "Main Distribution Center")
        String warehouseName,

        @Schema(description = "The timestamp when the order was issued to the supplier")
        LocalDateTime orderDate,

        @Schema(description = "The estimated timestamp when the stock is expected to arrive")
        LocalDateTime expectedDate,

        @Schema(description = "List of specific products and quantities expected in this shipment")
        List<OrderItemDetail> items
) {
    /**
     * Detailed view of a single product line item within a purchase order.
     */
    @Schema(description = "Itemized product details for the purchase order")
    public record OrderItemDetail(
            @Schema(description = "The internal UUID of the product",
                    example = "p-9988-x123")
            String productId,

            @Schema(description = "The display name of the product",
                    example = "Dell Latitude 5420")
            String productName,

            @Schema(description = "The Stock Keeping Unit (SKU) used for inventory matching",
                    example = "ELEC-LAP-DELL-01")
            String sku,

            @Schema(description = "The total quantity of units ordered",
                    example = "25")
            Integer quantity
    ) {}
}