package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ConsignmentAgreement;
import com.infotact.warehouse.entity.enums.ConsignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsignmentAgreementRepository extends JpaRepository<ConsignmentAgreement, String> {

    Optional<ConsignmentAgreement> findByAgreementCodeAndWarehouseId(String code, String warehouseId);

    List<ConsignmentAgreement> findBySupplierIdAndWarehouseId(String supplierId, String warehouseId);

    List<ConsignmentAgreement> findByStatusAndWarehouseId(ConsignmentStatus status, String warehouseId);

    /** Returns all agreements for a warehouse regardless of status — tenant-safe list-all. */
    List<ConsignmentAgreement> findAllByWarehouseId(String warehouseId);

    /**
     * Find all ACTIVE agreements whose settlement cycle is due today or overdue.
     * Used by the settlement scheduler.
     */
    @Query("""
       SELECT a FROM ConsignmentAgreement a
       WHERE a.status = 'ACTIVE'
         AND a.warehouse.id = :warehouseId
         AND (
           (
             (SELECT COUNT(s) FROM ConsignmentSettlement s WHERE s.agreement = a) = 0
             AND a.effectiveFrom <= :today
           )
           OR
           (SELECT MAX(s.periodTo) FROM ConsignmentSettlement s WHERE s.agreement = a)
              <= :today
         )
       """)
    List<ConsignmentAgreement> findAgreementsDueForSettlement(
            @Param("warehouseId") String warehouseId,
            @Param("today") LocalDate today
    );

    /**
     * Find active agreements expiring on or before the given date.
     * Used to auto-terminate expired agreements.
     */
    @Query("""
           SELECT a FROM ConsignmentAgreement a
           WHERE a.status = 'ACTIVE'
             AND a.effectiveTo IS NOT NULL
             AND a.effectiveTo <= :today
           """)
    List<ConsignmentAgreement> findExpiredAgreements(@Param("today") LocalDate today);
}