package com.infotact.warehouse.repository;

import com.infotact.warehouse.dto.v1.response.ProductProfitResponse;
import com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse;
import com.infotact.warehouse.entity.SellingOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfitReportRepository extends JpaRepository<SellingOrderItem, String> {

    // ─────────────────────────────────────────────────────────────────────────
    // WAREHOUSE-WIDE PROFIT
    // ─────────────────────────────────────────────────────────────────────────

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse(
               CAST(WEEK(o.createdAt) AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(CASE WHEN i.consignment = false THEN i.sellPriceAtTimeOfOrder * i.quantity ELSE 0 END),
               SUM(CASE WHEN i.consignment = false THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(i.profit)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE o.warehouse.id = :warehouseId
             AND YEAR(o.createdAt) = :year
             AND o.status NOT IN ('CANCELLED')
           GROUP BY WEEK(o.createdAt), YEAR(o.createdAt)
           ORDER BY WEEK(o.createdAt)
           """)
    List<ProfitPeriodResponse> findWeeklyProfit(
            @Param("warehouseId") String warehouseId,
            @Param("year") int year
    );

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse(
               CAST(MONTH(o.createdAt) AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(CASE WHEN i.consignment = false THEN i.sellPriceAtTimeOfOrder * i.quantity ELSE 0 END),
               SUM(CASE WHEN i.consignment = false THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(i.profit)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE o.warehouse.id = :warehouseId
             AND YEAR(o.createdAt) = :year
             AND o.status NOT IN ('CANCELLED')
           GROUP BY MONTH(o.createdAt), YEAR(o.createdAt)
           ORDER BY MONTH(o.createdAt)
           """)
    List<ProfitPeriodResponse> findMonthlyProfit(
            @Param("warehouseId") String warehouseId,
            @Param("year") int year
    );

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProfitPeriodResponse(
               CAST(1 AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(CASE WHEN i.consignment = false THEN i.sellPriceAtTimeOfOrder * i.quantity ELSE 0 END),
               SUM(CASE WHEN i.consignment = false THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(CASE WHEN i.consignment = true  THEN i.profit ELSE 0 END),
               SUM(i.profit)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE o.warehouse.id = :warehouseId
             AND o.status NOT IN ('CANCELLED')
           GROUP BY YEAR(o.createdAt)
           ORDER BY YEAR(o.createdAt)
           """)
    List<ProfitPeriodResponse> findYearlyProfit(@Param("warehouseId") String warehouseId);

    // ─────────────────────────────────────────────────────────────────────────
    // PER-PRODUCT PROFIT
    // ─────────────────────────────────────────────────────────────────────────

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProductProfitResponse(
               i.product.id,
               i.product.name,
               i.product.sku,
               CAST(WEEK(o.createdAt) AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(i.quantity),
               SUM(i.sellPriceAtTimeOfOrder * i.quantity),
               SUM(i.costPriceAtTimeOfOrder * i.quantity),
               SUM(i.profit),
               CAST(i.consignment AS boolean)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE i.product.id = :productId
             AND o.warehouse.id = :warehouseId
             AND YEAR(o.createdAt) = :year
             AND o.status NOT IN ('CANCELLED')
           GROUP BY i.product.id, i.product.name, i.product.sku,
                    WEEK(o.createdAt), YEAR(o.createdAt), i.consignment
           ORDER BY WEEK(o.createdAt)
           """)
    List<ProductProfitResponse> findWeeklyProfitByProduct(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("year") int year
    );

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProductProfitResponse(
               i.product.id,
               i.product.name,
               i.product.sku,
               CAST(MONTH(o.createdAt) AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(i.quantity),
               SUM(i.sellPriceAtTimeOfOrder * i.quantity),
               SUM(i.costPriceAtTimeOfOrder * i.quantity),
               SUM(i.profit),
               CAST(i.consignment AS boolean)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE i.product.id = :productId
             AND o.warehouse.id = :warehouseId
             AND YEAR(o.createdAt) = :year
             AND o.status NOT IN ('CANCELLED')
           GROUP BY i.product.id, i.product.name, i.product.sku,
                    MONTH(o.createdAt), YEAR(o.createdAt), i.consignment
           ORDER BY MONTH(o.createdAt)
           """)
    List<ProductProfitResponse> findMonthlyProfitByProduct(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId,
            @Param("year") int year
    );

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProductProfitResponse(
               i.product.id,
               i.product.name,
               i.product.sku,
               CAST(1 AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(i.quantity),
               SUM(i.sellPriceAtTimeOfOrder * i.quantity),
               SUM(i.costPriceAtTimeOfOrder * i.quantity),
               SUM(i.profit),
               CAST(i.consignment AS boolean)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE i.product.id = :productId
             AND o.warehouse.id = :warehouseId
             AND o.status NOT IN ('CANCELLED')
           GROUP BY i.product.id, i.product.name, i.product.sku,
                    YEAR(o.createdAt), i.consignment
           ORDER BY YEAR(o.createdAt)
           """)
    List<ProductProfitResponse> findYearlyProfitByProduct(
            @Param("productId") String productId,
            @Param("warehouseId") String warehouseId
    );

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY — all products ranked by profit
    // ─────────────────────────────────────────────────────────────────────────

    @Query("""
           SELECT new com.infotact.warehouse.dto.v1.response.ProductProfitResponse(
               i.product.id,
               i.product.name,
               i.product.sku,
               CAST(0 AS integer),
               CAST(YEAR(o.createdAt) AS integer),
               SUM(i.quantity),
               SUM(i.sellPriceAtTimeOfOrder * i.quantity),
               SUM(i.costPriceAtTimeOfOrder * i.quantity),
               SUM(i.profit),
               CAST(i.consignment AS boolean)
           )
           FROM SellingOrderItem i
           JOIN i.order o
           WHERE o.warehouse.id = :warehouseId
             AND YEAR(o.createdAt) = :year
             AND (:month = 0 OR MONTH(o.createdAt) = :month)
             AND o.status NOT IN ('CANCELLED')
           GROUP BY i.product.id, i.product.name, i.product.sku,
                    YEAR(o.createdAt), i.consignment
           ORDER BY SUM(i.profit) DESC
           """)
    List<ProductProfitResponse> findTopProductsByProfit(
            @Param("warehouseId") String warehouseId,
            @Param("year") int year,
            @Param("month") int month
    );
}