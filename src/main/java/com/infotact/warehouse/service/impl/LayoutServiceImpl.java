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
 * This service manages the physical structural hierarchy (Warehouse -> Zone -> Aisle -> Bin).
 * It utilizes a "Lazy Loading" pattern: the core layout provides structural metrics,
 * while granular Bin data is fetched via paginated sub-queries to ensure UI performance.
 * </p>
 * * @author Gemini
 * @version 2.1
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
     * Retrieves the high-level warehouse structure.
     * <p>
     * <b>Optimization:</b> This method stops at the Aisle level. It calculates total
     * capacity/occupancy for the dashboard but does not load the Bin collection
     * to prevent payload bloat.
     * </p>
     *
     * @param id The Warehouse UUID.
     * @return WarehouseLayoutResponse containing Zone and Aisle summaries.
     */
    @Override
    @Transactional(readOnly = true)
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        log.info("Fetching optimized warehouse layout for ID: {}", id);

        Warehouse warehouse = warehouseRepository.findByIdWithZones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));

        List<WarehouseLayoutResponse.ZoneSummary> zoneDtos = warehouse.getZones().stream()
                .map(this::mapToZoneDto)
                .sorted(Comparator.comparing(WarehouseLayoutResponse.ZoneSummary::getName))
                .toList();

        return WarehouseLayoutResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .zones(zoneDtos)
                .totalCapacity(zoneDtos.stream().mapToDouble(z -> z.getTotalCapacity()).sum())
                .currentOccupancy(zoneDtos.stream().mapToDouble(z -> z.getCurrentOccupancy()).sum())
                .build();
    }

    /**
     * Provides "Drill-Down" access to bins within a specific aisle.
     * <p>
     * Implementation of the Lazy Loading pattern. Bins are fetched with
     * physical saturation math (Volume vs Weight) included.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        log.debug("Lazy loading bins for Aisle: {}", aisleId);
        return binRepository.findByAisleId(aisleId, pageable)
                .map(this::mapToBinSummary);
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
        Double cap = warehouseRepository.sumCapacityByAisleId(aisle.getId());
        Double occ = warehouseRepository.sumOccupancyByAisleId(aisle.getId());

        return WarehouseLayoutResponse.AisleSummary.builder()
                .id(aisle.getId())
                .code(aisle.getCode())
                .active(aisle.isActive())
                .bins(Collections.emptySet()) // Bins are loaded via getBinsByAisle
                .totalCapacity(cap != null ? cap : 0.0)
                .currentOccupancy(occ != null ? occ : 0.0)
                .build();
    }

    /**
     * Maps an entity to a DTO with physical utilization logic.
     * The fillPercentage reflects the "Most Restrictive Constraint" (Volume or Weight).
     */
    private WarehouseLayoutResponse.BinSummary mapToBinSummary(StorageBin bin) {
        double volUsage = (bin.getMaxVolume() > 0)
                ? (bin.getCurrentVolumeOccupied() / bin.getMaxVolume()) * 100 : 0;

        double weightUsage = (bin.getMaxWeightCapacity() > 0)
                ? (bin.getCurrentWeightLoad() / bin.getMaxWeightCapacity()) * 100 : 0;

        // UI progress bar shows whichever saturation is higher
        int finalFillPercent = (int) Math.round(Math.max(volUsage, weightUsage));

        return WarehouseLayoutResponse.BinSummary.builder()
                .id(bin.getId())
                .binCode(bin.getBinCode())
                .capacity(bin.getMaxVolume())
                .currentOccupancy(bin.getCurrentVolumeOccupied())
                .maxWeight(bin.getMaxWeightCapacity())
                .currentWeight(bin.getCurrentWeightLoad())
                .fillPercentage(finalFillPercent)
                .active(bin.isActive())
                .build();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addZoneToWarehouse(ZoneRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        if(zoneRepository.existsByNameAndWarehouseId(request.name(), request.warehouseId())){
            throw new AlreadyExistsException("Zone '" + request.name() + "' exists in this warehouse.");
        }

        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setWarehouse(warehouse);
        zone.setActive(true);
        zoneRepository.save(zone);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void addAisleToZone(AisleRequest request) {
        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));

        if (aisleRepository.existsByCodeAndZoneId(request.code(), request.zoneId())){
            throw new AlreadyExistsException("Aisle '" + request.code() + "' exists in this zone.");
        }

        Aisle aisle = new Aisle();
        aisle.setCode(request.code());
        aisle.setZone(zone);
        aisle.setActive(true);
        aisleRepository.save(aisle);
    }

    /**
     * Automates the bulk provisioning of storage bins.
     * Includes automated naming sequence and physical constraint injection.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void bulkCreateBins(BulkBinRequest request) {
        Aisle aisle = aisleRepository.findById(request.aisleId())
                .orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));

        Warehouse warehouse = aisle.getZone().getWarehouse();
        List<StorageBin> bins = new ArrayList<>();
        int sequence = 1;
        int createdCount = 0;

        while (createdCount < request.quantity()) {
            String generatedCode = String.format("%s-%03d", request.prefix(), sequence);

            if (!binRepository.existsByBinCode(generatedCode)) {
                StorageBin bin = StorageBin.builder()
                        .binCode(generatedCode)
                        .maxVolume(request.defaultMaxVolume())
                        .maxWeightCapacity(request.defaultMaxWeight())
                        .currentVolumeOccupied(0.0)
                        .currentWeightLoad(0.0)
                        .aisle(aisle)
                        .warehouse(warehouse)
                        .status(BinStatus.AVAILABLE)
                        .active(true)
                        .build();
                bins.add(bin);
                createdCount++;
            }
            sequence++;
            if (sequence > 999) throw new IllegalOperationException("Prefix sequence limit exceeded.");
        }
        binRepository.saveAll(bins);
        log.info("Bulk creation complete: {} bins added to Aisle {}", createdCount, aisle.getCode());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateZoneStatus(String zoneId, boolean isActive) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        zone.setActive(isActive);
        zoneRepository.save(zone);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateAisleStatus(String aisleId, boolean isActive) {
        Aisle aisle = aisleRepository.findById(aisleId).orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));
        aisle.setActive(isActive);
        aisleRepository.save(aisle);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateBinStatus(String binId, boolean isActive) {
        StorageBin bin = binRepository.findById(binId).orElseThrow(() -> new ResourceNotFoundException("Storage Bin not found"));
        bin.setActive(isActive);
        binRepository.save(bin);
    }
}