package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence entity representing a master catalog item.
 * <p>
 * This entity serves as the template for all inventory tracked within a specific
 * facility. It stores static attributes (price, weight, SKU) and operational
 * parameters (minThreshold) used by the dashboard for stock-level alerting.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@DynamicInsert
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_product_active", columnList = "active"),
        @Index(name = "idx_product_warehouse", columnList = "warehouse_id")
})
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    /**
     * Stock Keeping Unit (SKU).
     * <p>
     * Logic: A unique business identifier. Unlike the UUID, this is used for
     * human-readable tracking, barcode printing, and supplier communication.
     * </p>
     */
    @Column(unique = true, nullable = false)
    private String sku;

    private String description;

    /**
     * Unit price of the product.
     * <p>
     * Precision: Uses 19,4 to support high-accuracy financial calculations
     * and multi-currency support if needed later.
     * </p>
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /** Physical weight in KG. Used for calculating shipping costs and bin load limits. */
    private Double weight;

    /** Industry-standard barcode (EAN, UPC) for handheld scanner integration. */
    private String barcode;

    /** * Status flag.
     * <p>
     * Logic: Soft-delete implementation. Inactive products remain in history
     * but are hidden from the active catalog and cannot be ordered.
     * </p>
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Safety stock level.
     * <p>
     * Logic: When total inventory falls below this number, the system triggers
     * a 'LOW_STOCK' alert on the dashboard.
     * </p>
     */
    @Column(nullable = false)
    private Integer minThreshold = 10;

    /** Hierarchical category for grouping and reporting. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    /**
     * DENORMALIZATION: Direct link to Warehouse.
     * <p>
     * Performance: Allows the system to quickly fetch all products belonging to a
     * specific facility without traversing the complex Layout hierarchy.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /** The physical instances of this product stored across various bins. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryItem> inventoryItems;
}