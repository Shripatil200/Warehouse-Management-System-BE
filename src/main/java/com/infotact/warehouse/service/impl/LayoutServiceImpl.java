package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.*;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.BarcodeService;
import com.infotact.warehouse.service.LayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
 * Manages the structural hierarchy (Warehouse -> Zone -> Aisle -> Bin).
 * Optimized for high-performance UI rendering through:
 * <ul>
 *     <li><b>Consolidated Aggregation:</b> One query fetches all structural metrics.</li>
 *     <li><b>Lazy Loading:</b> Granular bin data is only fetched when a user selects an aisle.</li>
 *     <li><b>Null Safety:</b> Guaranteed default values (0.0 or 0) to prevent UI breakage.</li>
 * </ul>
 * </p>
 *
 * @author Gemini
 * @version 3.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayoutServiceImpl implements LayoutService {

    private final BinRepository binRepository;
    private final ZoneRepository zoneRepository;
    private final AisleRepository aisleRepository;
    private final WarehouseRepository warehouseRepository;

    private final BarcodeService barcodeService;
    /**
     * Retrieves the structural layout with pre-calculated metrics (Capacity, Occupancy, Bin Count).
     * <p>
     * <b>Update Note:</b> Uses explicit entity joins in the repository to ensure bin counts
     * are never returned as null.
     * </p>
     *
     * @param id The Warehouse UUID.
     * @return WarehouseLayoutResponse containing hierarchical summaries.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouseLayouts", key = "#id")
    public WarehouseLayoutResponse getWarehouseLayout(String id) {
        log.info("Generating layout metrics for Warehouse: {}", id);

        // 1. Fetch Structural Skeleton (Warehouse -> Zones -> Aisles)
        Warehouse warehouse = warehouseRepository.findOptimizedLayout(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));

        // 2. Fetch Aggregated Metrics (Capacity, Occupancy, Bin Count)
        List<Object[]> metricsResults = warehouseRepository.findAllAisleMetricsByWarehouseId(id);

        // 3. Map Metrics for O(1) lookup during DTO construction
        Map<String, AisleMetrics> metricsMap = metricsResults.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new AisleMetrics(
                        (row[1] == null) ? 0.0 : ((Number) row[1]).doubleValue(),
                        (row[2] == null) ? 0.0 : ((Number) row[2]).doubleValue(),
                        (row[3] == null) ? 0L : ((Number) row[3]).longValue()
                ),
                (existing, replacement) -> existing
        ));

        // 4. Build hierarchical DTO
        List<WarehouseLayoutResponse.ZoneSummary> zoneDtos = warehouse.getZones().stream()
                .map(zone -> mapToZoneDto(zone, metricsMap))
                .sorted(Comparator.comparing(WarehouseLayoutResponse.ZoneSummary::getName))
                .toList();

        return WarehouseLayoutResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .zones(zoneDtos)
                .totalCapacity(zoneDtos.stream().mapToDouble(WarehouseLayoutResponse.ZoneSummary::getTotalCapacity).sum())
                .currentOccupancy(zoneDtos.stream().mapToDouble(WarehouseLayoutResponse.ZoneSummary::getCurrentOccupancy).sum())
                .build();
    }

    /**
     * Provides drill-down access to specific bins.
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        log.debug("Drilling down into bins for Aisle: {}", aisleId);
        return binRepository.findByAisleId(aisleId, pageable)
                .map(this::mapToBinSummary);
    }

    /**
     * Maps a Zone and its child Aisles into a DTO summary.
     */
    private WarehouseLayoutResponse.ZoneSummary mapToZoneDto(Zone zone, Map<String, AisleMetrics> metricsMap) {
        Set<WarehouseLayoutResponse.AisleSummary> aisleDtos = zone.getAisles().stream()
                .map(aisle -> {
                    // Fetch metric record or use a default 'empty' record to ensure no nulls
                    AisleMetrics m = metricsMap.getOrDefault(aisle.getId(), new AisleMetrics(0.0, 0.0, 0L));

                    return WarehouseLayoutResponse.AisleSummary.builder()
                            .id(aisle.getId())
                            .code(aisle.getCode())
                            .active(aisle.isActive())
                            .bins(Collections.emptySet()) // Bins are lazy-loaded via getBinsByAisle
                            .totalCapacity(m.capacity())
                            .currentOccupancy(m.occupancy())
                            // Convert Long count from DB to Integer for DTO
                            .binCount(m.binCount() != null ? m.binCount().intValue() : 0)
                            .build();
                })
                .collect(Collectors.toSet());

        return WarehouseLayoutResponse.ZoneSummary.builder()
                .id(zone.getId())
                .name(zone.getName())
                .active(zone.isActive())
                .aisles(aisleDtos)
                .totalCapacity(aisleDtos.stream().mapToDouble(WarehouseLayoutResponse.AisleSummary::getTotalCapacity).sum())
                .currentOccupancy(aisleDtos.stream().mapToDouble(WarehouseLayoutResponse.AisleSummary::getCurrentOccupancy).sum())
                .build();
    }

    /**
     * Maps granular bin data with utilization logic.
     */
    private WarehouseLayoutResponse.BinSummary mapToBinSummary(StorageBin bin) {
        double volUsage = (bin.getMaxVolume() > 0)
                ? (bin.getCurrentVolumeOccupied() / bin.getMaxVolume()) * 100 : 0;

        double weightUsage = (bin.getMaxWeightCapacity() > 0)
                ? (bin.getCurrentWeightLoad() / bin.getMaxWeightCapacity()) * 100 : 0;

        // Take the highest occupancy ratio to represent 'fill' state
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
    @CacheEvict(value = "warehouseLayouts", key = "#request.warehouseId()")
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
    @CacheEvict(value = "warehouseLayouts", key = "#request.warehouseId()")
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

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "warehouseLayouts", allEntries = true)
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
            if (sequence > 999) throw new IllegalOperationException("Sequence limit exceeded.");
        }
        binRepository.saveAll(bins);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @CacheEvict(value = "warehouseLayouts", allEntries = true)
    public void updateZoneStatus(String zoneId, boolean isActive) {
        Zone zone = zoneRepository.findById(zoneId).orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        zone.setActive(isActive);
        zoneRepository.save(zone);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @CacheEvict(value = "warehouseLayouts", allEntries = true)
    public void updateAisleStatus(String aisleId, boolean isActive) {
        Aisle aisle = aisleRepository.findById(aisleId).orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));
        aisle.setActive(isActive);
        aisleRepository.save(aisle);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @CacheEvict(value = "warehouseLayouts", allEntries = true)
    public void updateBinStatus(String binId, boolean isActive) {
        StorageBin bin = binRepository.findById(binId).orElseThrow(() -> new ResourceNotFoundException("Storage Bin not found"));
        bin.setActive(isActive);
        binRepository.save(bin);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getBinBarcode(String binId) {
        // Audit check: Ensure the bin exists and is active before providing a label[cite: 1]
        StorageBin bin = binRepository.findById(binId)
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));

        if (!bin.isActive()) {
            throw new IllegalOperationException("Cannot generate labels for inactive storage locations.");
        }

        return barcodeService.getBarcodeForBin(bin.getBinCode());
    }

    /**
     * Industry-Ready Feature: Scanned barcode validation.
     * Used by mobile scanners to verify the worker is at the correct rack.
     */
    @Override
    public boolean verifyBinScan(String scannedCode, String expectedBinId) {
        StorageBin bin = binRepository.findById(expectedBinId)
                .orElseThrow(() -> new ResourceNotFoundException("Expected bin not found"));

        return bin.getBinCode().equals(scannedCode);
    }

    /**
     * Internal record to encapsulate aggregate aisle metrics.
     * Use of records ensures immutability and thread-safety during mapping.
     */
    private record AisleMetrics(Double capacity, Double occupancy, Long binCount) {}
}