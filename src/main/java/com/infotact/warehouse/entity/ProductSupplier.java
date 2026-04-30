package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Persistence entity mapping Products to multiple Suppliers.
 * <p>
 * <b>Logic:</b> One product can be provided by multiple vendors. This entity
 * stores the current supply quote and lead time, allowing Managers to
 * make data-driven decisions on which supplier to use for new Purchase Orders.
 * </p>
 */
@Data
@Entity
@Table(name = "product_suppliers")
public class ProductSupplier extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** The current purchase price offered by this supplier. */
    @Column(precision = 19, scale = 4)
    private BigDecimal currentSupplyPrice;

    /** Expected days for delivery from this specific vendor. */
    private Integer leadTimeDays;
}