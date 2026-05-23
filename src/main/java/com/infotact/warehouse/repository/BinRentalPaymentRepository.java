package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.SupplierRevenueResponse;
import com.infotact.warehouse.entity.BinRentalPayment;
import com.infotact.warehouse.entity.enums.BinRentalPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BinRentalPaymentRepository extends JpaRepository<BinRentalPayment, String> {

    List<BinRentalPayment> findAllByBinRentalId(String binRentalId);

    List<BinRentalPayment> findAllByBinRentalIdAndStatus(String binRentalId, BinRentalPaymentStatus status);

    /**
     * Returns true when a payment record already covers the requested period for the given rental.
     * Guards against double-billing by the scheduler.
     */
    boolean existsByBinRentalIdAndPeriodFromAndPeriodTo(String binRentalId, LocalDate periodFrom, LocalDate periodTo);

    /**
     * Aggregates total bin-rental revenue per supplier within the warehouse for a date range.
     * Used by the Supplier Revenue Report.
     */
    @Query("""
           SELECT brp.binRental.supplier.id,
                  SUM(brp.totalAmount)
           FROM BinRentalPayment brp
           WHERE brp.binRental.warehouse.id = :warehouseId
             AND brp.periodFrom >= :from
             AND brp.periodTo   <= :to
           GROUP BY brp.binRental.supplier.id
           """)
    List<Object[]> sumRentalRevenuePerSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Aggregates bin-rental revenue for a single supplier within the warehouse for a date range.
     */
    @Query("""
           SELECT COALESCE(SUM(brp.totalAmount), 0)
           FROM BinRentalPayment brp
           WHERE brp.binRental.warehouse.id = :warehouseId
             AND brp.binRental.supplier.id = :supplierId
             AND brp.periodFrom >= :from
             AND brp.periodTo   <= :to
           """)
    java.math.BigDecimal sumRentalRevenueForSupplier(
            @Param("warehouseId") String warehouseId,
            @Param("supplierId") String supplierId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    /**
     * Total bin-rental revenue for the warehouse in a date range (for dashboard).
     */
    @Query("""
           SELECT COALESCE(SUM(brp.totalAmount), 0)
           FROM BinRentalPayment brp
           WHERE brp.binRental.warehouse.id = :warehouseId
             AND brp.periodFrom >= :from
             AND brp.periodTo   <= :to
           """)
    java.math.BigDecimal sumTotalRentalRevenue(
            @Param("warehouseId") String warehouseId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
