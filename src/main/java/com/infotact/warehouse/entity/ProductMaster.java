package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Layer 1 — Global product definition, independent of any warehouse or supplier.
 * <p>
 * {@code ProductMaster} represents the real-world identity of a product (what it is),
 * without any commercial or warehouse-specific data. It is the single source of truth
 * for product name, description, barcode, and physical specs shared across all tenants.
 * </p>
 * <ul>
 *   <li>Multiple suppliers can offer the same ProductMaster via {@link SupplierProduct}.</li>
 *   <li>Multiple warehouses can stock the same ProductMaster via their own {@link Product} (WarehouseProduct).</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "product_master",
        indexes = {
                @Index(name = "idx_pm_barcode",  columnList = "barcode"),
                @Index(name = "idx_pm_category", columnList = "category_id")
        }
)
public class ProductMaster extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** The universal product name (e.g., "AA Alkaline Battery 1.5V"). */
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Industry-standard barcode (EAN-13, UPC-A, etc.) for scanner integration. */
    @Column(length = 100)
    private String barcode;

    /** Broad classification for browsing and reporting. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    /** Unit of Measure (e.g., PCS, BOX, KG, LTR). */
    @Column(nullable = false, length = 20)
    private String uom;

    /** Weight in kilograms. */
    private Double weight;

    /** Length in centimetres. */
    private Double length;

    /** Width in centimetres. */
    private Double width;

    /** Height in centimetres. */
    private Double height;
}