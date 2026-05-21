package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ConsignmentSettlement;
import com.infotact.warehouse.entity.enums.ConsignmentSettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsignmentSettlementRepository extends JpaRepository<ConsignmentSettlement, String> {

    List<ConsignmentSettlement> findByAgreementIdOrderByPeriodFromDesc(String agreementId);

    List<ConsignmentSettlement> findByStatusAndWarehouseId(
            ConsignmentSettlementStatus status, String warehouseId);

    Optional<ConsignmentSettlement> findBySettlementNumberAndWarehouseId(
            String settlementNumber, String warehouseId);

    /**
     * The most recent settlement period end for an agreement.
     * Used by the scheduler to determine the next cycle start.
     */
    @Query("""
           SELECT MAX(s.periodTo) FROM ConsignmentSettlement s
           WHERE s.agreement.id = :agreementId
           """)
    Optional<LocalDate> findLastSettledPeriodEnd(@Param("agreementId") String agreementId);
}
