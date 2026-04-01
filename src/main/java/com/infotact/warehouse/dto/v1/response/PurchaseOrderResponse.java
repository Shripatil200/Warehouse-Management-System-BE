package com.infotact.warehouse.dto.v1.response;

import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderResponse(
        String id,
        String supplierName,
        String status,
        LocalDateTime orderDate,
        List<OrderItemDetail> items
) {
    public record OrderItemDetail(
            String productId,
            String productName,
            String sku,
            Integer quantity
    ) {
    }
}