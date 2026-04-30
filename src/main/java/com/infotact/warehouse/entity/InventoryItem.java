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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Persistence entity representing specific physical stock at a location.
 * <p>
 * Tracks quantities, batch details, and individual serial numbers. Uses
 * Optimistic Locking to handle high-concurrency picking/receiving operations.
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
                        name = "uk_product_bin_status_batch_serial",
                        columnNames = {"product_id", "storage_bin_id", "status", "batch_number", "serial_number"}
                )
        },
        indexes = {
                @Index(name = "idx_inv_product", columnList = "product_id"),
                @Index(name = "idx_inv_bin", columnList = "storage_bin_id"),
                @Index(name = "idx_inv_expiry", columnList = "expiryDate")
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

    /** * Manufacturer batch identifier. Default is "NONE".
     */
    @Column(name = "batch_number", nullable = false)
    private String batchNumber = "NONE";

    /** * Unique unit identifier. Required if product.isSerialized is true.
     */
    @Column(name = "serial_number")
    private String serialNumber;

    /** * Crucial for FEFO (First-Expired, First-Out) logic.
     */
    private LocalDate expiryDate;

    /** * Historic purchase cost for this specific stock entry.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    /**
     * Calculates net pickable stock.
     */
    public Long getAvailableQuantity() {
        if (this.status != InventoryStatus.AVAILABLE) {
            return 0L;
        }
        return Math.max(0L, (long) this.quantity - this.reservedQuantity);
    }
}