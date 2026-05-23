package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.BinRental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BinRentalRepository extends JpaRepository<BinRental, String> {

    List<BinRental> findAllByWarehouseIdAndActiveTrue(String warehouseId);

    List<BinRental> findAllBySupplierIdAndWarehouseId(String supplierId, String warehouseId);

    Optional<BinRental> findByIdAndWarehouseId(String id, String warehouseId);

    @Query("""
           SELECT br FROM BinRental br
           WHERE br.warehouse.id = :warehouseId
             AND br.active = true
             AND br.storageBin.id = :binId
           """)
    Optional<BinRental> findActiveRentalForBin(
            @Param("warehouseId") String warehouseId,
            @Param("binId") String binId
    );

    /**
     * Finds all active rentals whose billing window has not yet been closed for today.
     * Used by the billing scheduler to generate periodic payment records.
     */
    @Query("""
           SELECT br FROM BinRental br
           WHERE br.warehouse.id = :warehouseId
             AND br.active = true
             AND br.startDate <= :today
             AND (br.endDate IS NULL OR br.endDate >= :today)
           """)
    List<BinRental> findBillableRentals(
            @Param("warehouseId") String warehouseId,
            @Param("today") LocalDate today
    );
}
