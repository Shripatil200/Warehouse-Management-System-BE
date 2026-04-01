package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

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

    @Column(nullable = false, unique = true)
    private String binCode; // e.g., "BIN-A01-S1-01"

    @Column(nullable = false)
    private Integer capacity; // Max volume/units this bin can hold

    @Builder.Default
    @Column(nullable = false)
    private Integer currentOccupancy = 0; // Current units stored

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BinStatus status; // Industry standard: AVAILABLE, FULL, BLOCKED, RESERVED

    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", nullable = false)
    private Aisle aisle;

    @OneToMany(mappedBy = "storageBin", cascade = CascadeType.ALL)
    private List<InventoryItem> inventoryItems;

    @Version
    private Long version; // Optimistic Locking for high-concurrency environments

    // Helper method for Industry-level logic
    public boolean canAccommodate(int quantity) {
        return active && (currentOccupancy + quantity <= capacity);
    }
}