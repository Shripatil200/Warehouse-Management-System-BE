package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * Maps a single {@link Product} into a {@link ConsignmentAgreement}.
 *
 * <p>This entity stores the agreed commercial terms for each product line:
 * the MRP (maximum retail price) the supplier sets, and an optional floor
 * price below which the warehouse must not discount.
 *
 * <p>The {@code Product} linked here must already exist in the warehouse
 * catalog. Product records for consigned goods are created the same way as
 * warehouse-owned goods — the ownership model is determined solely by the
 * presence of a {@link ConsignmentProduct} record pointing to this entity.
 *
 * <p><b>Ownership flag on Product:</b>
 * A separate boolean {@code isConsignment} on the {@link Product} entity
 * (added as part of this feature) allows the system to quickly determine
 * at query time whether any given product is warehouse-owned or supplier-owned
 * without a join to this table.
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "consignment_products",
        uniqueConstraints = {
                // One product can only appear once per agreement
                @UniqueConstraint(
                        name = "uk_consignment_product",
                        columnNames = {"agreement_id", "product_id"}
                )
        }
)
public class ConsignmentProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Parent agreement this product line belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private ConsignmentAgreement agreement;

    /**
     * The product being consigned.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Maximum Retail Price (MRP) set by the supplier.
     * The warehouse may not sell above this price.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal mrp;

    /**
     * Floor price set by the supplier.
     * The warehouse may not sell below this price without explicit supplier approval.
     * Null means no floor restriction.
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal floorPrice;

    /**
     * Whether this product line is currently active within the agreement.
     * Allows suspending a single product without terminating the whole agreement.
     */
    @Column(nullable = false)
    private boolean active = true;
}
