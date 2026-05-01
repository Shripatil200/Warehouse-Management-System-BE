package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence entity representing a master product in the catalog.
 * <p>
 * This entity stores physical, financial, and operational metadata. It is
 * designed for multi-tenant isolation, ensuring SKUs are unique per warehouse.
 * </p>
 * <p>
 * <b>Optimization:</b> Includes a cached {@code unitVolume} field to
 * accelerate bin-capacity calculations during putaway operations.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DynamicUpdate
@DynamicInsert
@Entity
@Table(
        name = "products",
        indexes = {
                // Scoped index: SKU is unique WITHIN a warehouse
                @Index(name = "idx_product_sku_warehouse", columnList = "sku, warehouse_id"),
                @Index(name = "idx_product_active", columnList = "active"),
                @Index(name = "idx_product_warehouse", columnList = "warehouse_id")
        }
)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    /**
     * Stock Keeping Unit.
     * Note: Unique constraint is now handled via the composite index with warehouse_id.
     */
    @Column(nullable = false)
    private String sku;

    @Column(length = 1000)
    private String description;

    // --- Financials ---

    /** Unit Selling Price (Price charged to customer) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal sellingPrice;

    /** Unit Cost Price (What the warehouse paid; used for valuation) */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal costPrice;

    // --- Logistics & Physical Specs ---

    /** Unit of Measure (e.g., PCS, BOX, KG, LTR) */
    @Column(nullable = false, length = 20)
    private String uom;

    private Double weight; // in KG
    private Double length; // in cm
    private Double width;  // in cm
    private Double height; // in cm

    /**
     * Pre-calculated volume (cm³).
     * Used to verify if a Product fits into a StorageBin.
     */
    @Column(name = "unit_volume", precision = 19, scale = 4)
    private Double unitVolume;

    /** Industry-standard barcode for scanner integration */
    private String barcode;

    // --- Operational Controls ---

    @Column(nullable = false)
    private boolean active = true;

    /** Minimum stock level before triggering alert */
    @Column(nullable = false)
    private Integer minThreshold = 10;

    /** Maximum stock level to prevent overstocking */
    private Integer maxThreshold;

    // --- Traceability Flags ---

    /** If true, every single unit must have a unique Serial Number */
    private boolean isSerialized = false;

    /** If true, items are tracked by Batch/Lot (e.g., food or pharma) */
    private boolean isBatchTracked = false;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    /**
     * Strict multi-tenancy: The warehouse that owns this product record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<InventoryItem> inventoryItems;

    // --- Lifecycle Hooks ---

    /**
     * Automatically calculates the volumetric footprint of the product
     * before persisting or updating.
     */
    @PrePersist
    @PreUpdate
    private void calculateUnitVolume() {
        if (length != null && width != null && height != null) {
            this.unitVolume = length * width * height;
        } else {
            this.unitVolume = 0.0;
        }
    }
}