package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.IllegalOperationException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.AisleRepository;
import com.infotact.warehouse.repository.BinRepository;
import com.infotact.warehouse.repository.WarehouseRepository;
import com.infotact.warehouse.repository.ZoneRepository;
import com.infotact.warehouse.service.LayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private final BinRepository binRepository;

    private final ZoneRepository zoneRepository;

    private final AisleRepository aisleRepository;

    private final WarehouseRepository warehouseRepository;


    @Override
    public void addZoneToWarehouse(ZoneRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse with id" + request.warehouseId() + " not found"));



        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setWarehouse(warehouse);
        zoneRepository.save(zone);
        log.info("feat: added new zone {} to warehouse {}", request.name(), request.warehouseId());
    }

    @Override
    public void addAisleToZone(AisleRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone with id: " + request.zoneId() + " not fount"));

        if(!zone.getWarehouse().getId().equals(request.warehouseId())){
            throw new BadRequestException("Security Breach/Data Mismatch: Zone " + request.zoneId() +
                    " does not belong to Warehouse " + request.warehouseId());
        }

        // 3. If valid, proceed with creation
        Aisle aisle = new Aisle();
        aisle.setCode(request.code());
        aisle.setZone(zone);

        aisleRepository.save(aisle);
    }

    @Override
    @Transactional
    public void bulkCreateBins(BulkBinRequest request) {
        log.info("Initiating industry-level bulk bin creation: {} bins for Aisle ID: {}",
                request.quantity(), request.aisleId());

        // 1. Fetch the Aisle and validate the hierarchy
        // We fetch the Aisle, which also gives us access to its Parent Zone
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new ResourceNotFoundException("Aisle with id: " + request.aisleId() + " not found"));

        // 2. SECURITY CHAIN VALIDATION
        // Ensure the aisle belongs to the specified Zone and Warehouse provided in the request
        // This prevents accidental data corruption or cross-warehouse ID injections
        if (!aisle.getZone().getId().equals(request.zoneId())) {
            throw new BadRequestException("Data Mismatch: Aisle " + request.aisleId() +
                    " does not belong to Zone " + request.zoneId());
        }

        if (!aisle.getZone().getWarehouse().getId().equals(request.warehouseId())) {
            throw new BadRequestException("Security Breach: Zone/Aisle hierarchy does not match Warehouse " +
                    request.warehouseId());
        }


        List<StorageBin> bins = new ArrayList<>();
        int sequence = 1;
        int createdCount = 0;

        while (createdCount < request.quantity()) {
            String generatedCode = String.format("%s-%03d", request.prefix(), sequence);

            // INDUSTRY FIX: Check if this code exists ANYWHERE in the database
            // not just in this specific aisle.
            if (!binRepository.existsByBinCode(generatedCode)) {
                StorageBin bin = StorageBin.builder()
                        .binCode(generatedCode)
                        .capacity(request.defaultCapacity())
                        .aisle(aisle)
                        .status(BinStatus.AVAILABLE)
                        .currentOccupancy(0)
                        .active(true)
                        .build();

                bins.add(bin);
                createdCount++;
            }

            sequence++;

            // Safety break: Prevent infinite loop if prefix is bad
            if (sequence > 999) {
                throw new IllegalOperationException("Prefix range exceeded or too many duplicates found.");
            }
        }

        binRepository.saveAll(bins);
        log.info("Successfully bulk-created {} bins", bins.size());
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
