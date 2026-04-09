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

/**
 * Persistence entity representing physical stock of a specific product at a specific location.
 * <p>
 * This is the core transactional entity of the warehouse. It tracks quantities,
 * reservations, and quality status. To maintain data integrity, it uses Optimistic
 * Locking and a strict unique constraint on Product + Bin + Status + Batch.
 * </p>
 */
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
                @Index(name = "idx_inv_expiry", columnList = "expiryDate")
        }
)
public class InventoryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Optimistic Locking version field.
     * <p>
     * Prevents "Lost Updates" when multiple workers attempt to update the
     * same stock record simultaneously (e.g., two pickers grabbing from the same bin).
     * </p>
     */
    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "storage_bin_id", nullable = false)
    private StorageBin storageBin;

    /** Total physical count currently sitting in the bin. */
    @Column(nullable = false)
    private Integer quantity = 0;

    /** * Quantity committed to active outbound orders but not yet physically picked.
     * This quantity is "frozen" and cannot be sold to others.
     */
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    /** * The quality/availability state (e.g., AVAILABLE, DAMAGED, QUARANTINED).
     * Damaged goods are excluded from 'Pickable' calculations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    /** * Manufacturer batch/lot identifier. Default is "NONE".
     * Part of the unique constraint to allow batch-level tracking.
     */
    @Column(name = "batch_number", nullable = false)
    private String batchNumber = "NONE";

    /** * Product expiration date.
     * Crucial for FEFO (First-Expired, First-Out) picking strategies.
     */
    private LocalDate expiryDate;

    /**
     * Business Logic: Calculates the quantity eligible for new orders.
     * <p>
     * Returns 0 if the status is not AVAILABLE.
     * Otherwise, returns (Physical Quantity - Reserved Quantity).
     * </p>
     * @return Long representing net pickable stock.
     */
    public Long getAvailableQuantity() {
        if (this.status != InventoryStatus.AVAILABLE) {
            return 0L;
        }
        return Math.max(0L, (long) this.quantity - this.reservedQuantity);
    }
}