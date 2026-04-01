package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    // Find all orders for a specific supplier
    List<PurchaseOrder> findAllBySupplierId(String supplierId);

    // Find orders by status (e.g., PLACED, RECEIVED)
    List<PurchaseOrder> findAllByStatus(String status);
}