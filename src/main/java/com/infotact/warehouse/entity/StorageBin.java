package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.BinStatus;
import com.infotact.warehouse.entity.enums.BinType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

/**
 * Persistence entity representing the smallest physical storage unit in the facility.
 * <p>
 * This entity tracks real-time occupancy based on physical dimensions (cm³)
 * and weight limits (KG). It implements Optimistic Locking to prevent capacity
 * overruns during simultaneous stock movements.
 * </p>
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_bins", indexes = {
        @Index(name = "idx_bin_aisle", columnList = "aisle_id"),
        @Index(name = "idx_bin_status", columnList = "status"),
        @Index(name = "idx_bin_warehouse", columnList = "warehouse_id")
})
@EqualsAndHashCode(callSuper = true, exclude = {"aisle", "warehouse", "inventoryItems"})
@Builder
public class StorageBin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique alphanumeric code for the bin (e.g., "A1-S02-B05").
     * Typically matches the physical barcode label on the shelf.
     */
    @Column(nullable = false, unique = true)
    private String binCode;

    /** * The engineered volume limit of the bin in cubic centimeters (cm³).
     */
    @Column(nullable = false)
    private Double maxVolume;

    /** * Running total of volume currently occupied by products.
     */
    @Builder.Default
    @Column(nullable = false)
    private Double currentVolumeOccupied = 0.0;

    /** * The maximum weight load this bin can support in Kilograms (KG).
     */
    @Column(nullable = false)
    private Double maxWeightCapacity;

    /** * Total weight currently resting in the bin.
     */
    @Builder.Default
    @Column(nullable = false)
    private Double currentWeightLoad = 0.0;

    /** * State of the bin (AVAILABLE, FULL, BLOCKED, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "bin_type", nullable = false, length = 20)
    private BinType binType = BinType.PICK_FACE; // Default value

    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    /**
     * Multi-Tenant Isolation: Direct link to the parent Warehouse.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToMany(mappedBy = "storageBin", cascade = CascadeType.ALL)
    private List<InventoryItem> inventoryItems;

    @Version
    private Long version;

    /**
     * Validates if the bin can physically accommodate new stock.
     * * @param volume Required volume in cm³
     * @param weight Required weight in KG
     * @return true if the bin is active, available, and has remaining capacity.
     */
    public boolean canAccommodate(Double volume, Double weight) {
        return active &&
                status == BinStatus.AVAILABLE &&
                (currentVolumeOccupied + volume <= maxVolume) &&
                (currentWeightLoad + weight <= maxWeightCapacity);
    }
}