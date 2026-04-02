package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.ZoneRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.AisleRequest;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.BulkBinRequest;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.exception.AlreadyExistsException;
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

/**
 * Implementation of {@link LayoutService}.
 * Ensures structural integrity by validating parent-child relationships
 * across the Warehouse-Zone-Aisle-Bin hierarchy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private final BinRepository binRepository;
    private final ZoneRepository zoneRepository;
    private final AisleRepository aisleRepository;
    private final WarehouseRepository warehouseRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addZoneToWarehouse(ZoneRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        // Rule: Zone names must be unique within a single Warehouse facility
        if(zoneRepository.existsByNameAndWarehouseId(request.name(), request.warehouseId())){
            throw new AlreadyExistsException("Zone with name: " + request.name() + " already exists");
        }

        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setWarehouse(warehouse);
        zoneRepository.save(zone);
        log.info("feat: added new zone {} to warehouse {}", request.name(), request.warehouseId());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addAisleToZone(AisleRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

        // Security Validation: Ensure the Zone is physically inside the provided Warehouse
        if(!zone.getWarehouse().getId().equals(request.warehouseId())){
            throw new BadRequestException("Security Breach/Data Mismatch: Zone does not belong to Warehouse");
        }

        if (aisleRepository.existsByCodeAndZoneId(request.code(), request.zoneId())){
            throw new AlreadyExistsException("Aisle with code: " + request.code() + " already exists");
        }

        Aisle aisle = new Aisle();
        aisle.setCode(request.code());
        aisle.setZone(zone);
        aisleRepository.save(aisle);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void bulkCreateBins(BulkBinRequest request) {
        log.info("Initiating bulk bin creation: {} bins for Aisle ID: {}", request.quantity(), request.aisleId());

        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));

        // Strict Hierarchy Check: Validate full chain from Aisle up to Warehouse
        if (!aisle.getZone().getId().equals(request.zoneId()) ||
                !aisle.getZone().getWarehouse().getId().equals(request.warehouseId())) {
            throw new BadRequestException("Security Breach: Provided hierarchy IDs do not match database records.");
        }

        List<StorageBin> bins = new ArrayList<>();
        int sequence = 1;
        int createdCount = 0;

        // Iterative generation: Finds the next available numeric suffix for the prefix
        while (createdCount < request.quantity()) {
            String generatedCode = String.format("%s-%03d", request.prefix(), sequence);

            // Global Uniqueness Check: Ensures no duplicate bin codes across the entire facility
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

            // Circuit breaker to prevent infinite loops in case of high collision/bad prefix
            if (sequence > 999) {
                throw new IllegalOperationException("Prefix range exceeded (Max 999). Try a different prefix.");
            }
        }

        binRepository.saveAll(bins);
        log.info("Successfully bulk-created {} bins", bins.size());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        // Build the hierarchical response DTO manually from the entity graph
        WarehouseLayoutResponse response = new WarehouseLayoutResponse();
        response.setId(warehouse.getId());
        response.setName(warehouse.getName());

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

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        return binRepository.findByAisleId(aisleId, pageable)
                .map(bin -> WarehouseLayoutResponse.BinSummary.builder()
                        .id(bin.getId())
                        .binCode(bin.getBinCode())
                        .capacity(bin.getCapacity())
                        .build());
    }
}