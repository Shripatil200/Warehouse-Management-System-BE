package com.infotact.warehouse.dto.v1.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder // <--- This allows the .builder() call in your Service
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseLayoutResponse {
    private String id;
    private String name;
    private List<ZoneSummary> zones;

    @Data
    @Builder // <--- This allows the .builder() call in your Service
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneSummary {
        private String id;
        private String name;
        private List<AisleSummary> aisles;
    }

    @Data
    @Builder // <--- This allows the .builder() call in your Service
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AisleSummary {
        private String id;
        private String code;
        private List<BinSummary> bins;
    }

    @Data
    @Builder // <--- This allows the .builder() call in your Service
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinSummary {
        private String id;
        private String binCode;
        private Integer capacity;
    }
}