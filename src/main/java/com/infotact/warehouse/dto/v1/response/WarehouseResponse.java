package com.infotact.warehouse.dto.v1.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class WarehouseResponse {
    private String id;
    private String name;
    private String location;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int zoneCount; // Useful for UI dashboards
}