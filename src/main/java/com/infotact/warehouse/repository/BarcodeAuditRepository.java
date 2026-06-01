package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.BarcodeAudit;
import com.infotact.warehouse.entity.enums.AuditStatus;
import com.infotact.warehouse.entity.enums.AuditAction;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BarcodeAuditRepository extends JpaRepository<BarcodeAudit, String> {

    Optional<BarcodeAudit> findFirstByOrderIdAndStatusOrderByTimestampDesc(String orderId, AuditStatus status);

    Optional<BarcodeAudit> findFirstByOrderIdAndActionTypeAndStatusOrderByTimestampDesc(
            String orderId, AuditAction actionType, AuditStatus status);

    List<BarcodeAudit> findAllByWarehouseId(String warehouseId);

    List<BarcodeAudit> findAllByUserId(String userId);

    // Used for performance reporting
    long countByUserIdAndStatus(String userId, AuditStatus status);
    /**
     * Finds barcode audit logs with optional filters for User and Status.
     */
    @Query("""
    SELECT b FROM BarcodeAudit b
    WHERE b.warehouse.id = :warehouseId
      AND (:userId IS NULL OR b.userId = :userId)
      AND (:status IS NULL OR b.status = :status)
""")
    Page<BarcodeAudit> findFilteredAudits(
            @Param("warehouseId") String warehouseId,
            @Param("status") AuditStatus status,
            @Param("userId") String userId,
            Pageable pageable
    );
}
