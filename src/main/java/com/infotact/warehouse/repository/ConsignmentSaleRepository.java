package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.ConsignmentSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsignmentSaleRepository extends JpaRepository<ConsignmentSale, String> {

    /**
     * Find all unsettled sales for an agreement within a time window.
     * Used during settlement generation.
     */
    @Query("""
           SELECT cs FROM ConsignmentSale cs
           WHERE cs.agreement.id = :agreementId
             AND cs.settled = false
             AND cs.soldAt >= :from
             AND cs.soldAt <= :to
           ORDER BY cs.soldAt ASC
           """)
    List<ConsignmentSale> findUnsettledSalesForPeriod(
            @Param("agreementId") String agreementId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Bulk mark sales as settled and link them to a settlement record.
     */
    @Modifying
    @Query("""
           UPDATE ConsignmentSale cs
           SET cs.settled = true, cs.settlement.id = :settlementId
           WHERE cs.id IN :ids
           """)
    void markAsSettled(@Param("ids") List<String> ids, @Param("settlementId") String settlementId);

    List<ConsignmentSale> findByAgreementIdOrderBySoldAtDesc(String agreementId);
}
