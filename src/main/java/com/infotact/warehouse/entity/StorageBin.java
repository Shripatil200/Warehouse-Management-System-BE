package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "storage_bins")
@EqualsAndHashCode(callSuper = true)
public class StorageBin extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String binCode; // e.g., "BIN-A01-S1-01"

    private Integer capacity; // Max quantity this bin can hold

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id")
    private Aisle aisle;

    @OneToMany(mappedBy = "storageBin")
    private List<InventoryItem> inventoryItems;
}