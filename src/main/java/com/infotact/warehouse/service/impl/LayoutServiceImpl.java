package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.dto.v1.request.WarehouseLayoutRequest.*;
import com.infotact.warehouse.dto.v1.response.WarehouseLayoutResponse;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.BinType;
import com.infotact.warehouse.entity.enums.ZoneType;
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
 * Production-grade tenant-safe implementation of {@link LayoutService}.
 *
 * <p>
 * Enforces strict multi-tenant isolation:
 * <ul>
 *     <li>No external warehouseId usage</li>
 *     <li>All operations scoped via {@link TenantContext}</li>
 *     <li>Cross-entity ownership validation enforced</li>
 * </ul>
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
    private final BarcodeService barcodeService;

    // ============================================================
    // READ: FULL LAYOUT
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "warehouseLayouts",
            key = "T(com.infotact.warehouse.config.TenantContext).get()"
    )
    public WarehouseLayoutResponse getWarehouseLayout() {
        String warehouseId = getCurrentWarehouseId();
        log.info("Generating layout for tenant warehouse: {}", warehouseId);

        Warehouse warehouse = warehouseRepository.findOptimizedLayout(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        List<Object[]> metricsResults =
                warehouseRepository.findAllAisleMetricsByWarehouseId(warehouseId);

        Map<String, AisleMetrics> metricsMap = metricsResults.stream().collect(Collectors.toMap(
                row -> (String) row[0],
                row -> new AisleMetrics(
                        (row[1] == null) ? 0.0 : ((Number) row[1]).doubleValue(),
                        (row[2] == null) ? 0.0 : ((Number) row[2]).doubleValue(),
                        (row[3] == null) ? 0L : ((Number) row[3]).longValue()
                )
        ));

        List<WarehouseLayoutResponse.ZoneSummary> zoneDtos =
                warehouse.getZones().stream()
                        .map(zone -> mapToZoneDto(zone, metricsMap))
                        .toList();

        return WarehouseLayoutResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .zones(zoneDtos)
                .totalCapacity(zoneDtos.stream().mapToDouble(WarehouseLayoutResponse.ZoneSummary::getTotalCapacity).sum())
                .currentOccupancy(zoneDtos.stream().mapToDouble(WarehouseLayoutResponse.ZoneSummary::getCurrentOccupancy).sum())
                .build();
    }

    // ============================================================
    // READ: BINS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<WarehouseLayoutResponse.BinSummary> getBinsByAisle(String aisleId, Pageable pageable) {
        Aisle aisle = getValidatedAisle(aisleId);
        return binRepository.findByAisleIdAndWarehouseId(
                aisle.getId(),
                getCurrentWarehouseId(),
                pageable
        ).map(this::mapToBinSummary);
    }

    // ============================================================
    // WRITE: ZONE
    // ============================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(
            value = "warehouseLayouts",
            key = "T(com.infotact.warehouse.config.TenantContext).get()"
    )
    public void addZoneToWarehouse(ZoneRequest request) {
        Warehouse warehouse = getCurrentWarehouse();

        if (zoneRepository.existsByNameAndWarehouseId(request.name(), warehouse.getId())) {
            throw new AlreadyExistsException("Zone already exists in warehouse");
        }

        Zone zone = new Zone();
        zone.setName(request.name());
        zone.setZoneType(request.zoneType());
        zone.setWarehouse(warehouse); // Correctly set tenant link
        zone.setActive(true);

        zoneRepository.save(zone);
    }

    // ============================================================
    // WRITE: AISLE
    // ============================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(
            value = "warehouseLayouts",
            key = "T(com.infotact.warehouse.config.TenantContext).get()"
    )
    public void addAisleToZone(AisleRequest request) {
        // Retrieve and validate the zone belongs to this tenant
        Zone zone = getValidatedZone(request.zoneId());

        if (aisleRepository.existsByCodeAndZoneId(request.code(), zone.getId())) {
            throw new AlreadyExistsException("Aisle exists in zone");
        }

        // FIX: Retrieve current warehouse (tenant) to satisfy NOT NULL constraint
        Warehouse warehouse = getCurrentWarehouse();

        Aisle aisle = new Aisle();
        aisle.setCode(request.code());
        aisle.setZone(zone);
        aisle.setWarehouse(warehouse); // LINKING TO TENANT MANUALLY
        aisle.setActive(true);

        aisleRepository.save(aisle);
    }

    // ============================================================
    // WRITE: BULK BINS
    // ============================================================

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(
            value = "warehouseLayouts",
            key = "T(com.infotact.warehouse.config.TenantContext).get()"
    )
    public void bulkCreateBins(BulkBinRequest request) {
        Aisle aisle = getValidatedAisle(request.aisleId());
        Warehouse warehouse = aisle.getZone().getWarehouse();
        ZoneType zoneType = aisle.getZone().getZoneType();

        BinType defaultType = (zoneType == ZoneType.BULK) ? BinType.BULK_STORAGE : BinType.PICK_FACE;
        BinType finalType = request.binTypeOverride() != null ? request.binTypeOverride() : defaultType;

        List<StorageBin> bins = new ArrayList<>();
        int sequence = 1;
        int created = 0;

        while (created < request.quantity()) {
            String code = String.format("%s-%03d", request.prefix(), sequence);

            if (!binRepository.existsByBinCodeAndWarehouseId(code, warehouse.getId())) {
                StorageBin bin = StorageBin.builder()
                        .binCode(code)
                        .binType(finalType)
                        .maxVolume(request.defaultMaxVolume())
                        .maxWeightCapacity(request.defaultMaxWeight())
                        .currentVolumeOccupied(0.0)
                        .currentWeightLoad(0.0)
                        .aisle(aisle)
                        .warehouse(warehouse) // Correctly set tenant link
                        .status(BinStatus.AVAILABLE)
                        .active(true)
                        .build();

                bins.add(bin);
                created++;
            }

            sequence++;
            if (sequence > 999) throw new IllegalOperationException("Sequence overflow");
        }
        binRepository.saveAll(bins);
    }

    // ============================================================
    // STATUS UPDATES & BARCODES
    // ============================================================

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void updateZoneStatus(String zoneId, boolean isActive) {
        getValidatedZone(zoneId).setActive(isActive);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void updateAisleStatus(String aisleId, boolean isActive) {
        getValidatedAisle(aisleId).setActive(isActive);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void updateBinStatus(String binId, boolean isActive) {
        getValidatedBin(binId).setActive(isActive);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getBinBarcode(String binId) {
        StorageBin bin = getValidatedBin(binId);
        if (!bin.isActive()) throw new IllegalOperationException("Bin inactive");
        return barcodeService.getBarcodeForBin(bin.getBinCode());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyBinScan(String scannedCode, String expectedBinId) {
        return binRepository.findById(expectedBinId)
                .filter(bin -> bin.getWarehouse().getId().equals(getCurrentWarehouseId()))
                .map(bin -> bin.isActive() && bin.getBinCode().equalsIgnoreCase(scannedCode.trim()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public String getBinCodeById(String binId) {
        return getValidatedBin(binId).getBinCode();
    }

    // ============================================================
    // VALIDATION HELPERS
    // ============================================================

    private Warehouse getCurrentWarehouse() {
        return warehouseRepository.findById(getCurrentWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
    }

    private Zone getValidatedZone(String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found"));
        if (!zone.getWarehouse().getId().equals(getCurrentWarehouseId())) {
            throw new SecurityException("Cross-tenant zone access denied");
        }
        return zone;
    }

    private Aisle getValidatedAisle(String aisleId) {
        Aisle aisle = aisleRepository.findById(aisleId)
                .orElseThrow(() -> new ResourceNotFoundException("Aisle not found"));
        if (!aisle.getZone().getWarehouse().getId().equals(getCurrentWarehouseId())) {
            throw new SecurityException("Cross-tenant aisle access denied");
        }
        return aisle;
    }

    private StorageBin getValidatedBin(String binId) {
        StorageBin bin = binRepository.findById(binId)
                .orElseThrow(() -> new ResourceNotFoundException("Bin not found"));
        if (!bin.getWarehouse().getId().equals(getCurrentWarehouseId())) {
            throw new SecurityException("Cross-tenant bin access denied");
        }
        return bin;
    }

    private String getCurrentWarehouseId() {
        String id = TenantContext.get();
        if (id == null) throw new IllegalStateException("Tenant missing");
        return id;
    }

    // ============================================================
    // MAPPING
    // ============================================================

    private WarehouseLayoutResponse.ZoneSummary mapToZoneDto(Zone zone, Map<String, AisleMetrics> metricsMap) {
        Set<WarehouseLayoutResponse.AisleSummary> aisles =
                zone.getAisles().stream()
                        .map(aisle -> {
                            AisleMetrics m = metricsMap.getOrDefault(aisle.getId(), new AisleMetrics(0.0, 0.0, 0L));
                            return WarehouseLayoutResponse.AisleSummary.builder()
                                    .id(aisle.getId())
                                    .code(aisle.getCode())
                                    .active(aisle.isActive())
                                    .totalCapacity(m.capacity())
                                    .currentOccupancy(m.occupancy())
                                    .binCount(m.binCount().intValue())
                                    .build();
                        }).collect(Collectors.toSet());

        return WarehouseLayoutResponse.ZoneSummary.builder()
                .id(zone.getId())
                .name(zone.getName())
                .zoneType(zone.getZoneType())
                .active(zone.isActive())
                .aisles(aisles)
                .totalCapacity(aisles.stream().mapToDouble(WarehouseLayoutResponse.AisleSummary::getTotalCapacity).sum())
                .currentOccupancy(aisles.stream().mapToDouble(WarehouseLayoutResponse.AisleSummary::getCurrentOccupancy).sum())
                .build();
    }

    private WarehouseLayoutResponse.BinSummary mapToBinSummary(StorageBin bin) {
        double vol = bin.getMaxVolume() > 0 ? (bin.getCurrentVolumeOccupied() / bin.getMaxVolume()) * 100 : 0;
        double weight = bin.getMaxWeightCapacity() > 0 ? (bin.getCurrentWeightLoad() / bin.getMaxWeightCapacity()) * 100 : 0;
        int fill = (int) Math.round(Math.max(vol, weight));

        return WarehouseLayoutResponse.BinSummary.builder()
                .id(bin.getId())
                .binCode(bin.getBinCode())
                .binType(bin.getBinType())
                .capacity(bin.getMaxVolume())
                .currentOccupancy(bin.getCurrentVolumeOccupied())
                .maxWeight(bin.getMaxWeightCapacity())
                .currentWeight(bin.getCurrentWeightLoad())
                .fillPercentage(fill)
                .active(bin.isActive())
                .build();
    }

    private record AisleMetrics(Double capacity, Double occupancy, Long binCount) {}
}