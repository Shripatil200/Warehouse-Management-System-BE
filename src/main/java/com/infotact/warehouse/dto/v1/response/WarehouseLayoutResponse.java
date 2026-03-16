package com.infotact.warehouse.dto.v1.response;

import lombok.Data;
import java.util.List;

@Data
public class WarehouseLayoutResponse {
    private String id;
    private String name;
    private List<ZoneSummary> zones;

    @Data
    public static class ZoneSummary {
        private String id;
        private String name;
        private List<AisleSummary> aisles;
    }

    @Data
    public static class AisleSummary {
        private String id;
        private String code;
        private List<BinSummary> bins;
    }

    @Data
    public static class BinSummary {
        private String id;
        private String binCode;
        private Integer capacity;
    }
}