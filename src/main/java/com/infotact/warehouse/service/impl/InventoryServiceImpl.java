package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.*;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.TransactionType;
import com.infotact.warehouse.exception.InsufficientStorageException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.*;
import com.infotact.warehouse.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;

    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Double totalVolume = (product.getLength() * product.getWidth() * product.getHeight()) * request.getQuantity();
        Double totalWeight = product.getWeight() * request.getQuantity();

        StorageBin targetBin = null;

        // Priority 1: User-specified Bin (Manual Placement/Scanning)
        if (request.getStorageBinId() != null && !request.getStorageBinId().isBlank()) {
            targetBin = binRepository.findById(request.getStorageBinId())
                    .orElseThrow(() -> new ResourceNotFoundException("Specified Bin not found"));

            if (!targetBin.canAccommodate(totalVolume, totalWeight)) {
                throw new InsufficientStorageException("Specified bin " + targetBin.getBinCode() + " cannot fit this shipment.");
            }
        }

        // Priority 2: Smart Putaway Engine (Directed Placement)
        if (targetBin == null) {
            String preferredZoneId = product.getCategory().getPreferredZoneId();
            List<StorageBin> bins = binRepository.findSmartPutawayBins(
                    product.getId(), preferredZoneId, totalVolume, totalWeight);

            if (bins.isEmpty()) {
                bins = binRepository.findSmartPutawayBins(product.getId(), null, totalVolume, totalWeight);
            }

            if (bins.isEmpty()) {
                throw new InsufficientStorageException("No suitable storage location found for this shipment footprint.");
            }
            targetBin = bins.get(0);
        }

        processPutaway(targetBin, product, request);
    }

    private void processPutaway(StorageBin bin, Product product, ReceivingRequest request) {
        int quantity = request.getQuantity();
        Double totalVol = (product.getLength() * product.getWidth() * product.getHeight()) * quantity;
        Double totalWeight = product.getWeight() * quantity;

        bin.setCurrentVolumeOccupied(bin.getCurrentVolumeOccupied() + totalVol);
        bin.setCurrentWeightLoad(bin.getCurrentWeightLoad() + totalWeight);

        if (bin.getCurrentVolumeOccupied() >= (bin.getMaxVolume() * 0.95)) {
            bin.setStatus(BinStatus.FULL);
        }
        binRepository.save(bin);

        InventoryItem item = inventoryRepository.findByProductIdAndStorageBinId(product.getId(), bin.getId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setStorageBin(bin);
                    newItem.setQuantity(0);
                    newItem.setBatchNumber(request.getBatchNumber() != null ? request.getBatchNumber() : "NONE");
                    newItem.setExpiryDate(request.getExpiryDate());
                    newItem.setPurchasePrice(request.getUnitCost() != null ? request.getUnitCost() :
                            (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO));
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + quantity);
        inventoryRepository.save(item);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventoryItem(item);
        transaction.setType(TransactionType.INBOUND);
        transaction.setQuantityChange((long) quantity);
        transaction.setUnitPrice(item.getPurchasePrice());
        transaction.setReferenceId(request.getBatchNumber());
        transaction.setReasonCode("GOODS_RECEIPT");
        transactionRepository.save(transaction);
    }
}