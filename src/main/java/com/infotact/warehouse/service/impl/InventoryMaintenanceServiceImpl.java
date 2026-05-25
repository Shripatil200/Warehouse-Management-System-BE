package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.WarehouseContext;
import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.service.InventoryMaintenanceService;
import com.infotact.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled maintenance tasks for inventory health.
 *
 * <p>This system has exactly one warehouse. The scheduler fetches that warehouse's
 * ID once via {@link WarehouseService#getSingleWarehouseId()}, sets
 * {@link WarehouseContext} (so Hibernate's warehouse filter activates), and
 * processes expired stock in batches of {@value #BATCH_SIZE}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryMaintenanceServiceImpl implements InventoryMaintenanceService {

    private static final int BATCH_SIZE = 200;

    private final InventoryRepository inventoryRepository;
    private final ExpiryProcessor     expiryProcessor;
    private final WarehouseService    warehouseService;

    // ── Scheduled entry point ────────────────────────────────────────────────

    @Override
    @Scheduled(cron = "0 0 2 * * ?")   // daily at 02:00
    @SchedulerLock(name = "expiry_job", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void quarantineExpiredStock() {

        log.info("===== EXPIRY JOB STARTED =====");

        String warehouseId;
        try {
            warehouseId = warehouseService.getSingleWarehouseId();
        } catch (IllegalStateException e) {
            log.info("Expiry job skipped — no warehouse configured yet.");
            return;
        }

        try {
            WarehouseContext.set(warehouseId);
            log.info("Processing warehouse {}", warehouseId);
            processWarehouse(warehouseId);
        } catch (Exception ex) {
            log.error("Expiry job failed for warehouse {}", warehouseId, ex);
        } finally {
            WarehouseContext.clear();
        }

        log.info("===== EXPIRY JOB COMPLETED =====");
    }

    // ── Core processing (batch-safe) ─────────────────────────────────────────

    private void processWarehouse(String warehouseId) {
        LocalDate today      = LocalDate.now();
        int totalProcessed   = 0;
        int batchCount       = 0;

        while (true) {
            List<InventoryItem> batch = inventoryRepository.lockNextExpiredBatch(
                    InventoryStatus.AVAILABLE.name(),
                    today,
                    warehouseId,
                    BATCH_SIZE
            );

            if (batch.isEmpty()) break;

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
            log.info("Batch {} processed, size={}", batchCount, batch.size());
        }

        log.info("Expiry job complete — total expired processed={}", totalProcessed);
    }
}
