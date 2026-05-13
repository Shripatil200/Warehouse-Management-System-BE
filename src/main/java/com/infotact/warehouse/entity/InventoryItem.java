package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.TenantAwareEntity;
import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_bin_batch_price_expiry",
                        // REMOVED 'status' and 'serial_number' from uniqueness
                        // ADDED 'expiry_date' to ensure layers are distinct
                        columnNames = {"product_id", "storage_bin_id", "batch_number", "purchase_price", "expiry_date"}
                )
        },
        indexes = {
                // Optimized for FEFO picking (Product + Expiry)
                @Index(name = "idx_inv_product_expiry", columnList = "product_id, expiryDate"),
                @Index(name = "idx_inv_bin", columnList = "storage_bin_id")
        }
)
@FilterDef(
        name = "warehouseFilter",
        parameters = @ParamDef(name = "warehouseId", type = String.class)
)
public class InventoryItem extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_bin_id", nullable = false)
    private StorageBin storageBin;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber = "NONE";

    /**
     * Serial number support:
     * Note: If you use Serial Numbers, you usually store quantity as 1.
     * For bulk stock, this remains null.
     */
    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * Helper to calculate pickable stock.
     */
    public Integer getAvailableQuantity() {
        // If status is not AVAILABLE, nothing is pickable
        if (this.status != InventoryStatus.AVAILABLE) {
            return 0;
        }
        // Logic: Physical minus Reserved, but never less than zero[cite: 1]
        return Math.max(0, this.quantity - this.reservedQuantity);
    }
}