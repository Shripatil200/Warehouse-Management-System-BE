package com.infotact.warehouse.dto.v1.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private Double weight;
    private String barcode;
    private boolean active;
    private String categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
