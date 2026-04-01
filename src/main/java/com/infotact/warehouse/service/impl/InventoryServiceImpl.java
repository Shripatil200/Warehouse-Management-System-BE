package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.BinStatus;
import com.infotact.warehouse.entity.InventoryItem;
import com.infotact.warehouse.entity.Product;
import com.infotact.warehouse.entity.StorageBin;
import com.infotact.warehouse.exception.InsufficientStorageException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.BinRepository;
import com.infotact.warehouse.repository.InventoryRepository;
import com.infotact.warehouse.repository.ProductRepository;
import com.infotact.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        log.info("Starting receiving process for Product ID: {} | Quantity: {}",
                request.getProductId(), request.getQuantity());

        // 1. Validate Product Existence
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        // 2. Putaway Algorithm: Find the first bin that can accommodate the quantity
        StorageBin targetBin = binRepository.findAvailableBinsForPutaway(request.getQuantity())
                .stream()
                .findFirst()
                .orElseThrow(() -> new InsufficientStorageException("No available bin space for quantity: " + request.getQuantity()));

        // 3. Atomic Update: Update Storage Bin Occupancy
        int newOccupancy = targetBin.getCurrentOccupancy() + request.getQuantity();
        targetBin.setCurrentOccupancy(newOccupancy);

        if (newOccupancy >= targetBin.getCapacity()) {
            targetBin.setStatus(BinStatus.FULL);
        }

        // Save the bin state - @Version handles optimistic locking here
        binRepository.save(targetBin);

        // 4. Atomic Update: Upsert Inventory Item
        // Using orElseGet is professional practice to avoid unnecessary object instantiation
        InventoryItem item = inventoryRepository.findByProductIdAndStorageBinId(product.getId(), targetBin.getId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setStorageBin(targetBin);
                    newItem.setQuantity(0);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + request.getQuantity());
        inventoryRepository.save(item);

        log.info("Successfully moved {} units of SKU: {} to Bin: {}",
                request.getQuantity(), product.getSku(), targetBin.getBinCode());

        // the sum of InventoryItems is our real-time source of truth.
    }
}