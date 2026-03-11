package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
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
public class StorageBin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String binCode;

    private Integer capacity;

    @ManyToOne
    private Aisle aisle;

    @OneToMany(mappedBy = "storageBin")
    private List<InventoryItem> inventoryItems;
}
