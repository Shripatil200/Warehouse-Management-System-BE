package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.*;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.LayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for warehouse layout orchestration.
 * <p>
 * This version uses an Aggregation Engine pattern, offloading capacity
 * calculations to the database to ensure high performance and null-safety.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private final BinRepository binRepository;
    private final ZoneRepository zoneRepository;
    private final AisleRepository aisleRepository;
    private final WarehouseRepository warehouseRepository;


    /**
     * {@inheritDoc}
     * <p>
     * <b>Process:</b> Fetches the warehouse structure and populates
     * Transient capacity fields via optimized SQL aggregations.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        Warehouse warehouse = warehouseRepository.findByIdWithZones(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        List<WarehouseLayoutResponse.ZoneSummary> zoneDtos = warehouse.getZones().stream()
                .map(this::mapToZoneDto)
                .sorted(Comparator.comparing(WarehouseLayoutResponse.ZoneSummary::getName))
                .toList();

        return WarehouseLayoutResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .zones(zoneDtos)
                .totalCapacity(zoneDtos.stream().mapToInt(z -> z.getTotalCapacity()).sum())
                .currentOccupancy(zoneDtos.stream().mapToInt(z -> z.getCurrentOccupancy()).sum())
                .build();
    }

    private WarehouseLayoutResponse.ZoneSummary mapToZoneDto(Zone zone) {
        Set<WarehouseLayoutResponse.AisleSummary> aisleDtos = zone.getAisles().stream()
                .map(this::mapToAisleDto)
                .collect(Collectors.toSet());

        return WarehouseLayoutResponse.ZoneSummary.builder()
                .id(zone.getId())
                .name(zone.getName())
                .active(zone.isActive())
                .aisles(aisleDtos)
                .totalCapacity(warehouseRepository.sumCapacityByZoneId(zone.getId()))
                .currentOccupancy(warehouseRepository.sumOccupancyByZoneId(zone.getId()))
                .build();
    }

    private WarehouseLayoutResponse.AisleSummary mapToAisleDto(Aisle aisle) {
        // 1. Fetch math from optimized repository queries
        Integer cap = warehouseRepository.sumCapacityByAisleId(aisle.getId());
        Integer occ = warehouseRepository.sumOccupancyByAisleId(aisle.getId());

        // 2. Populate the Transient fields in the Aisle Entity
        aisle.setTotalCapacity(cap != null ? cap : 0);
        aisle.setCurrentOccupancy(occ != null ? occ : 0);

        // 3. Map Bins only for detail views (Prevents massive JSON payloads)
        Set<WarehouseLayoutResponse.BinSummary> binDtos = aisle.getBins().stream()
                .map(bin -> WarehouseLayoutResponse.BinSummary.builder()
                        .id(bin.getId())
                        .binCode(bin.getBinCode())
                        .capacity(bin.getCapacity())
                        .currentOccupancy(bin.getCurrentOccupancy())
                        .active(bin.isActive())
                        .build())
                .collect(Collectors.toSet());

        return WarehouseLayoutResponse.AisleSummary.builder()
                .id(aisle.getId())
                .code(aisle.getCode())
                .active(aisle.isActive())
                .bins(binDtos)
                .totalCapacity(aisle.getTotalCapacity())
                .currentOccupancy(aisle.getCurrentOccupancy())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        return binRepository.findByAisleId(aisleId, pageable)
                .map(bin -> WarehouseLayoutResponse.BinSummary.builder()
                        .id(bin.getId())
                        .binCode(bin.getBinCode())
                        .capacity(bin.getCapacity())
                        .currentOccupancy(bin.getCurrentOccupancy())
                        .active(bin.isActive())
                        .build());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addZoneToWarehouse(ZoneRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        if(zoneRepository.existsByNameAndWarehouseId(request.name(), request.warehouseId())){
            throw new AlreadyExistsException("Zone with name: " + request.name() + " already exists");
        }

        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setWarehouse(warehouse);
        zone.setActive(true);
        zoneRepository.save(zone);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addAisleToZone(AisleRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

        if (aisleRepository.existsByCodeAndZoneId(request.code(), request.zoneId())){
            throw new AlreadyExistsException("Aisle with code: " + request.code() + " already exists");
        }

        Aisle aisle = new Aisle();
        aisle.setCode(request.code());
        aisle.setZone(zone);
        aisle.setActive(true);
        aisleRepository.save(aisle);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Note:</b> Uses zero-padded formatting for standardized
     * location codes. Skips existing codes for idempotency.
     * </p>
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkCreateBins(BulkBinRequest request) {
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));

        List<StorageBin> bins = new ArrayList<>();
        int sequence = 1;
        int createdCount = 0;

        while (createdCount < request.quantity()) {
            String generatedCode = String.format("%s-%03d", request.prefix(), sequence);
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
            if (sequence > 999) throw new IllegalOperationException("Prefix sequence range (999) exceeded.");
        }
        binRepository.saveAll(bins);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateZoneStatus(String zoneId, boolean isActive) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        zone.setActive(isActive);
        zoneRepository.save(zone);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateAisleStatus(String aisleId, boolean isActive) {
        Aisle aisle = aisleRepository.findById(aisleId).orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));
        aisle.setActive(isActive);
        aisleRepository.save(aisle);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateBinStatus(String binId, boolean isActive) {
        StorageBin bin = binRepository.findById(binId).orElseThrow(() -> new ResourceNotFoundException("Storage Bin not found"));
        bin.setActive(isActive);
        binRepository.save(bin);
    }
}