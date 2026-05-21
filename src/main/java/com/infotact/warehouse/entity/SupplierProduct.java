package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Layer 2 — A supplier's offering of a specific {@link ProductMaster}.
 * <p>
 * {@code SupplierProduct} captures the commercial relationship between a supplier
 * and a product: the price they charge and their typical lead time. Multiple
 * suppliers can each have a {@code SupplierProduct} row for the same
 * {@link ProductMaster}, enabling side-by-side price/lead-time comparison
 * when a warehouse manager raises a Purchase Order.
 * </p>
 * <p>
 * Note: This entity has no warehouse FK. It is intentionally global — the
 * same supplier offering is visible to all warehouses that search for it.
 * </p>
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
        name = "supplier_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sp_master_supplier",
                        columnNames = {"product_master_id", "supplier_id"}
                )
        },
        indexes = {
                @Index(name = "idx_sp_product_master", columnList = "product_master_id"),
                @Index(name = "idx_sp_supplier",       columnList = "supplier_id")
        }
)
public class SupplierProduct extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** The real-world product this offering describes. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_master_id", nullable = false)
    private ProductMaster productMaster;

    /**
     * The supplier (User with role=SUPPLIER) making this offering.
     * Using User entity directly keeps auth, profile, and catalogue in one table.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    /** The unit price this supplier charges for this product. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal supplyPrice;

    /** Expected delivery time in calendar days from order confirmation. */
    @Column(nullable = false)
    private Integer leadTimeDays = 0;

    /** Whether this offering is currently active and available for new POs. */
    @Column(nullable = false)
    private boolean active = true;
}