package com.infotact.warehouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Set;

/**
 * Persistence entity representing a physical aisle (row) within a warehouse zone.
 * <p>
 * An Aisle serves as a secondary organizational layer, grouping multiple
 * storage bins together. It is identified by a business code (e.g., "A-01")
 * and is always anchored to a specific {@link Zone}.
 * </p>
 */
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aisles")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Aisle extends WarehouseScopedEntity {

    @Id
    @EqualsAndHashCode.Include
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
     */
    private boolean active = true;

    /**
     * The parent zone where this aisle is located.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    @JsonIgnore
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
