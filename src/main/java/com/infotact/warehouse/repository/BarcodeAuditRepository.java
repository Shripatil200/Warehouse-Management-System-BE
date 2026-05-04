package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.BarcodeAudit;
import com.infotact.warehouse.entity.enums.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BarcodeAuditRepository extends JpaRepository<BarcodeAudit, String> {

    List<BarcodeAudit> findAllByWarehouseId(String warehouseId);

    List<BarcodeAudit> findAllByUserId(String userId);

    // Used for performance reporting
    long countByUserIdAndStatus(String userId, AuditStatus status);
}
