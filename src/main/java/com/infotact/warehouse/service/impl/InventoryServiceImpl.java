package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import com.infotact.warehouse.entity.enums.BinStatus;
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

import java.util.List;

/**
 * Implementation of {@link InventoryService} focusing on intelligent stock placement.
 * <p>
 * This service implements the 'Smart Putaway' algorithm, which optimizes warehouse
 * space by prioritizing product affinity zones before searching for global
 * fallback locations.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService {

    private final BinRepository binRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li><b>Affinity Logic:</b> Attempts to locate bins within the product's
     * Category-defined preferred zone to keep similar items together.</li>
     * <li><b>Fallback Mechanism:</b> If the preferred zone is saturated, the system
     * performs an automated global search for any available bin with sufficient capacity.</li>
     * <li><b>Transactional Integrity:</b> Ensures that if an {@link InsufficientStorageException}
     * is thrown, no partial stock updates occur.</li>
     * </ul>
     */
    @Override
    @Transactional
    public void receiveShipment(ReceivingRequest request) {
        log.info("Starting optimized receiving for Product ID: {} | Qty: {}",
                request.getProductId(), request.getQuantity());

        // 1. Validate Product & Category
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String preferredZoneId = product.getCategory().getPreferredZoneId();
        StorageBin targetBin = null;

        // 2. PHASE 1: Try Preferred Zone (Consolidation + Product Affinity)
        if (preferredZoneId != null) {
            List<StorageBin> preferredBins = binRepository.findSmartPutawayBins(
                    product.getId(), preferredZoneId, request.getQuantity());

            if (!preferredBins.isEmpty()) {
                targetBin = preferredBins.get(0);
                log.info("Preferred Zone Match: Selected Bin {}", targetBin.getBinCode());
            }
        }

        // 3. PHASE 2: Fallback - Global Search
        if (targetBin == null) {
            log.warn("Preferred zone {} is FULL or not defined. Searching all zones...", preferredZoneId);

            List<StorageBin> alternativeBins = binRepository.findSmartPutawayBins(
                    product.getId(), null, request.getQuantity());

            if (alternativeBins.isEmpty()) {
                throw new InsufficientStorageException("CRITICAL: Warehouse is 100% full. No space found.");
            }

            targetBin = alternativeBins.get(0);
            log.info("Force Putaway: Product diverted to Zone: {} | Bin: {}",
                    targetBin.getAisle().getZone().getName(), targetBin.getBinCode());
        }

        // 4. Atomic Updates
        processPutaway(targetBin, product, request.getQuantity());
    }

    /**
     * Executes the physical-to-digital state transition.
     * <p>
     * This helper handles the dual-update of the {@link StorageBin} occupancy
     * and the {@link InventoryItem} ledger.
     * </p>
     * @param bin The resolved target location.
     * @param product The product being received.
     * @param quantity The incoming amount.
     */
    private void processPutaway(StorageBin bin, Product product, Integer quantity) {
        // Update Bin Occupancy
        int newOccupancy = bin.getCurrentOccupancy() + quantity;
        bin.setCurrentOccupancy(newOccupancy);

        // Auto-toggle bin status if capacity is reached
        if (newOccupancy >= bin.getCapacity()) {
            bin.setStatus(BinStatus.FULL);
        }
        binRepository.save(bin);

        // Update/Upsert Inventory Item using an upsert pattern
        InventoryItem item = inventoryRepository.findByProductIdAndStorageBinId(product.getId(), bin.getId())
                .orElseGet(() -> {
                    InventoryItem newItem = new InventoryItem();
                    newItem.setProduct(product);
                    newItem.setStorageBin(bin);
                    newItem.setQuantity(0);
                    return newItem;
                });

        item.setQuantity(item.getQuantity() + quantity);
        inventoryRepository.save(item);

        log.info("Putaway Complete: SKU {} -> Bin {} (New Occupancy: {})",
                product.getSku(), bin.getBinCode(), newOccupancy);
    }
}