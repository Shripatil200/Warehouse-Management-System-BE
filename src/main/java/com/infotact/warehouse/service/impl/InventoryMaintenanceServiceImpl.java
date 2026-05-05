package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.service.InventoryMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryMaintenanceServiceImpl implements InventoryMaintenanceService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Runs at 2 AM every day
    public void quarantineExpiredStock() {
        log.info("System Task: Starting Expiry Guard sweep...");

        LocalDate today = LocalDate.now();

        // Use the FEFO-related logic already supported by your repository
        List<InventoryItem> expiredItems = inventoryRepository.findAllByStatusAndExpiryDateBefore(
                InventoryStatus.AVAILABLE, today);

        if (expiredItems.isEmpty()) {
            log.info("Expiry Guard: No expired stock detected.");
            return;
        }

        expiredItems.forEach(item -> {
            log.warn("ACTION REQUIRED: Stock Expired [ID: {}, SKU: {}, Batch: {}]",
                    item.getId(), item.getProduct().getSku(), item.getBatchNumber());
            item.setStatus(InventoryStatus.EXPIRED);
        });

        inventoryRepository.saveAll(expiredItems);
        log.info("Expiry Guard: Successfully quarantined {} items.", expiredItems.size());
    }
}