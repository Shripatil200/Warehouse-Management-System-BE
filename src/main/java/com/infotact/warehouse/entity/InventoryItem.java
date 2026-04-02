package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.enums.InventoryStatus;
import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Data
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
                        name = "uk_product_bin_status_batch",
                        columnNames = {"product_id", "storage_bin_id", "status", "batch_number"}
                )
        },
        indexes = {
                @Index(name = "idx_inv_product", columnList = "product_id"),
                @Index(name = "idx_inv_bin", columnList = "storage_bin_id"),
                @Index(name = "idx_inv_status", columnList = "status"),
                @Index(name = "idx_inv_expiry", columnList = "expiryDate") // Critical for FEFO picking
        }
)
public class InventoryItem extends BaseEntity {

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

    @Column(name = "batch_number", nullable = false) // Set to "NONE" if not applicable
    private String batchNumber = "NONE";

    private LocalDate expiryDate;

    /**
     * Business Logic: Returns what is actually pickable.
     * Note: If status is DAMAGED or QUARANTINED, this should
     * likely return 0 regardless of the math.
     */
    public Long getAvailableQuantity() {
        if (this.status != InventoryStatus.AVAILABLE) {
            return 0L;
        }
        return Math.max(0L, this.quantity - this.reservedQuantity);
    }
}