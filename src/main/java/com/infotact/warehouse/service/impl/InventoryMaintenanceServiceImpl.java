package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.config.TenantContext;
import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void quarantineExpiredStock() {

        List<String> warehouseIds = warehouseService.getAllWarehouseIds();

        log.info("Expiry Guard started for {} warehouses", warehouseIds.size());

        for (String warehouseId : warehouseIds) {

            try {
                TenantContext.set(warehouseId);

                log.info("Processing warehouse {}", warehouseId);

                processWarehouse(warehouseId);

            } catch (Exception ex) {
                log.error("Warehouse failed: {}", warehouseId, ex);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Expiry Guard completed");
    }

    private void processWarehouse(String warehouseId) {

        LocalDate today = LocalDate.now();
        int totalProcessed = 0;

        while (true) {

            List<InventoryItem> batch =
                    inventoryRepository.lockNextExpiredBatch(
                            InventoryStatus.AVAILABLE.name(),
                            today,
                            warehouseId,
                            BATCH_SIZE
                    );

            if (batch.isEmpty()) break;

            for (InventoryItem item : batch) {
                expiryProcessor.process(item.getId(), warehouseId);
            }

            totalProcessed += batch.size();

            log.info("Warehouse {} batch processed={}", warehouseId, batch.size());
        }

        log.info("Warehouse {} completed. Total={}", warehouseId, totalProcessed);
    }
}