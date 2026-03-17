package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.Warehouse;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.service.LayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {
    @Override
    public void addZoneToWarehouse(WarehouseLayoutRequest.ZoneRequest request) {

    }

    @Override
    public void addAisleToZone(WarehouseLayoutRequest.AisleRequest request) {

    }

    @Override
    public void bulkCreateBins(WarehouseLayoutRequest.BulkBinRequest request) {

    }




    @Override
    @Transactional(readOnly = true)
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        WarehouseLayoutResponse response = new WarehouseLayoutResponse();
        response.setId(warehouse.getId());
        response.setName(warehouse.getName());

        // Map Zones -> Aisles -> Bins (Hierarchical Catalog)
        if (warehouse.getZones() != null) {
            response.setZones(warehouse.getZones().stream().map(zone -> {
                var zoneDto = new WarehouseLayoutResponse.ZoneSummary();
                zoneDto.setId(zone.getId());
                zoneDto.setName(zone.getName());

                if (zone.getAisles() != null) {
                    zoneDto.setAisles(zone.getAisles().stream().map(aisle -> {
                        var aisleDto = new WarehouseLayoutResponse.AisleSummary();
                        aisleDto.setId(aisle.getId());
                        aisleDto.setCode(aisle.getCode());

                        if (aisle.getBins() != null) {
                            aisleDto.setBins(aisle.getBins().stream().map(bin -> {
                                var binDto = new WarehouseLayoutResponse.BinSummary();
                                binDto.setId(bin.getId());
                                binDto.setBinCode(bin.getBinCode());
                                binDto.setCapacity(bin.getCapacity());
                                return binDto;
                            }).toList());
                        }
                        return aisleDto;
                    }).toList());
                }
                return zoneDto;
            }).toList());
        }
        return response;
    }

    @Override
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        // 1. Call the REPOSITORY to get the Entities
        return binRepository.findByAisleId(aisleId, pageable)
                // 2. Map those Entities to your DTO (BinSummary)
                .map(bin -> WarehouseLayoutResponse.BinSummary.builder()
                        .id(bin.getId())
                        .binCode(bin.getBinCode())
                        .capacity(bin.getCapacity())
                        .build());
    }
}
