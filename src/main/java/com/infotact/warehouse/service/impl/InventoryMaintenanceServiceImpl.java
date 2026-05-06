package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryMaintenanceServiceImpl implements InventoryMaintenanceService {

    private static final int BATCH_SIZE = 200;

    private final InventoryRepository inventoryRepository;
    private final ExpiryProcessor expiryProcessor;
    private final WarehouseService warehouseService;

    // ============================================================
    // SCHEDULED ENTRY POINT (DISTRIBUTED SAFE)
    // ============================================================

    @Override
//    @Scheduled(fixedDelay = 10000)
    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 2 AM
    @SchedulerLock(name = "expiry_job", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void quarantineExpiredStock() {

        log.info("===== EXPIRY JOB STARTED =====");

        List<String> warehouseIds = warehouseService.getAllWarehouseIds();

        if (warehouseIds.isEmpty()) {
            log.info("No warehouses found. Skipping job.");
            return;
        }

        log.info("Expiry Guard started for {} warehouses", warehouseIds.size());

        for (String warehouseId : warehouseIds) {

            try {
                TenantContext.set(warehouseId);

                log.info("Processing warehouse {}", warehouseId);

                processWarehouse(warehouseId);

            } catch (Exception ex) {
                log.error("Warehouse processing failed: {}", warehouseId, ex);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("===== EXPIRY JOB COMPLETED =====");
    }

    // ============================================================
    // CORE PROCESSING (BATCH SAFE)
    // ============================================================

    private void processWarehouse(String warehouseId) {

        LocalDate today = LocalDate.now();
        int totalProcessed = 0;
        int batchCount = 0;

        while (true) {

            List<InventoryItem> batch =
                    inventoryRepository.lockNextExpiredBatch(
                            InventoryStatus.AVAILABLE.name(),
                            today,
                            warehouseId,
                            BATCH_SIZE
                    );

            if (batch.isEmpty()) {
                break;
            }

            batchCount++;

            for (InventoryItem item : batch) {
                try {
                    expiryProcessor.process(item.getId(), warehouseId);
                } catch (Exception ex) {
                    log.error("Failed to process item id={} in warehouse={}",
                            item.getId(), warehouseId, ex);
                }
            }

            totalProcessed += batch.size();

            log.info("Warehouse {} batch {} processed, size={}",
                    warehouseId, batchCount, batch.size());
        }

        log.info("Warehouse {} completed. Total expired processed={}",
                warehouseId, totalProcessed);
    }
}