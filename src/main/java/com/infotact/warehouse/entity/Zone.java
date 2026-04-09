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
 * Persistence entity representing a major functional area within a warehouse.
 * <p>
 * Zones are used to categorize storage space based on environmental requirements
 * (e.g., "Cold Storage") or product types (e.g., "Electronics").
 * It acts as the primary parent for all physical rows (Aisles) within that area.
 * </p>
 */
@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zones")
@EqualsAndHashCode(callSuper = true, exclude = {"warehouse", "aisles"})
public class Zone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Descriptive name of the zone.
     * <p>
     * Example: "Dry Goods", "Hazardous Materials", "Receiving Dock".
     * </p>
     */
    private String name;

    /**
     * Maintenance Status: Indicates if the zone is operational.
     * <p>
     * Logic: If 'false', system algorithms (Putaway/Picking) should
     * treat this entire area as inaccessible during maintenance windows.
     * </p>
     */
    private boolean active = true;

    /**
     * The parent warehouse facility containing this zone.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    /**
     * The set of physical aisles (rows) located within this zone.
     */
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL)
    private Set<Aisle> aisles;
}