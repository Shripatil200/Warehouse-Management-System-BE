package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.BinStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

/**
 * Persistence entity representing the smallest physical storage unit in the warehouse.
 * <p>
 * This entity tracks the physical capacity and real-time occupancy of a specific slot.
 * It is the final destination for products in the <b>Warehouse -> Zone -> Aisle -> Bin</b>
 * hierarchy. To ensure data integrity during simultaneous stock movements, it implements
 * Optimistic Locking.
 * </p>
 */
@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_bins", indexes = {
        @Index(name = "idx_bin_aisle", columnList = "aisle_id"),
        @Index(name = "idx_bin_status", columnList = "status")
})
@EqualsAndHashCode(callSuper = true)
@Builder
public class StorageBin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique alphanumeric code for the bin.
     * <p>
     * Example: "BIN-A01-S1-01". This code is typically printed as a barcode
     * on the physical shelf to be scanned during picking and put-away.
     * </p>
     */
    @Column(nullable = false, unique = true)
    private String binCode;

    /** * The maximum volume or unit count this bin is engineered to hold.
     */
    @Column(nullable = false)
    private Integer capacity;

    /** * The running total of items currently stored in this bin.
     * Logic: Updated dynamically by the InventoryService during stock movements.
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer currentOccupancy = 0;

    /** * Operational state of the bin.
     * <p>
     * Values include AVAILABLE (for put-away), FULL (at capacity),
     * BLOCKED (maintenance/damage), or RESERVED (staged for picking).
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinStatus status;

    /** * Soft-delete flag. Inactive bins are excluded from system-suggested
     * put-away logic.
     */
    @Builder.Default
    private boolean active = true;

    /** * The parent aisle containing this bin.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    /** * The actual inventory items (Product + Batch) currently residing in this bin.
     */
    @OneToMany(mappedBy = "storageBin", cascade = CascadeType.ALL)
    private List<InventoryItem> inventoryItems;

    /**
     * Optimistic Locking version.
     * <p>
     * Prevents race conditions where two workers might try to 'Put-Away' items
     * into the same bin at the same time, potentially exceeding capacity.
     * </p>
     */
    @Version
    private Long version;

    /**
     * Business Logic: Validates if the bin has sufficient remaining space.
     * * @param quantity The number of units intended to be added.
     * @return true if the bin is active and has enough volume available.
     */
    public boolean canAccommodate(int quantity) {
        return active && (currentOccupancy + quantity <= capacity);
    }
}