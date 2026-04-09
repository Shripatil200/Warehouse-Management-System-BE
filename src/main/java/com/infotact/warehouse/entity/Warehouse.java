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
 * Persistence entity representing a primary warehouse facility.
 * <p>
 * This is the top-level organizational unit of the system. It serves as the
 * parent container for physical infrastructure (Zones) and authorized staff (Users).
 * In this multi-tenant design, the Warehouse ID is the primary key used to
 * partition data across the entire platform.
 * </p>
 */
@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses")
@EqualsAndHashCode(callSuper = true)
public class Warehouse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Unique name of the facility.
     * <p>
     * Example: "Main Distribution Center - North", "Mumbai-Hub-01".
     * </p>
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Physical address or geographic coordinates of the facility.
     */
    private String location;

    /**
     * Operational status of the entire facility.
     * <p>
     * Logic: If set to false, the system should restrict all transactional
     * activity (receiving/picking) and potentially block user logins associated
     * with this specific warehouse.
     * </p>
     */
    private boolean active = true;

    /**
     * The physical regions defined within this warehouse.
     * <p>
     * Cascading: {@link CascadeType#ALL} ensures that deleting a warehouse
     * removes its entire physical hierarchy (Zones, Aisles, Bins) to prevent
     * orphaned records.
     * </p>
     */
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    private Set<Zone> zones;

    /**
     * The staff members assigned to work at this facility.
     * <p>
     * Relationship: A user is typically anchored to one warehouse to maintain
     * strict data access boundaries.
     * </p>
     */
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    private Set<User> users;
}