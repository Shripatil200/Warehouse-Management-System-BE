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
import java.util.Set;

/**
 * Persistence entity representing a physical aisle (row) within a warehouse zone.
 * <p>
 * An Aisle serves as a secondary organizational layer, grouping multiple
 * storage bins together. It is identified by a business code (e.g., "A-01")
 * and is always anchored to a specific {@link Zone}.
 * </p>
 */
@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aisles")
@EqualsAndHashCode(callSuper = true, exclude = {"zone", "bins"})
public class Aisle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Business identifier for the aisle.
     * <p>
     * Example: "A-01", "B-05".
     * </p>
     */
    @Column(nullable = false)
    private String code;

    /**
     * Maintenance Status: Indicates if this specific row is operational.
     * <p>
     * Logic: Used to temporarily block access to a specific row due to shelf
     * repairs or physical auditing.
     * </p>
     */
    private boolean active = true;

    /**
     * The parent zone where this aisle is located.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    /**
     * The collection of physical storage slots (bins) assigned to this aisle.
     */
    @OneToMany(mappedBy = "aisle", cascade = CascadeType.ALL)
    private Set<StorageBin> bins;

    /**
     * Calculated at runtime. Total storage potential of the row.
     */
    @Transient
    private Integer totalCapacity;

    /**
     * Calculated at runtime. Current unit count in the row.
     */
    @Transient
    private Integer currentOccupancy;

}