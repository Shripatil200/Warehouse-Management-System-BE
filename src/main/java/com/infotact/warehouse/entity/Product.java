package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.List;

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

    @Column(unique = true, nullable = false)
    private String sku;

    private String description;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryItem> inventoryItems;
}