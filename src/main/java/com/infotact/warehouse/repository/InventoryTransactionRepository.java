package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the immutable inventory ledger.
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
    // Find history for a specific inventory item
    List<InventoryTransaction> findAllByInventoryItemIdOrderByTransactionDateDesc(String inventoryItemId);
}