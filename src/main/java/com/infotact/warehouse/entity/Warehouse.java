package com.infotact.warehouse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;
import java.util.Set;

/**
 * Persistence entity representing the warehouse facility.
 *
 * <p>There is exactly one warehouse in this system. It is the top-level
 * organizational unit and parent container for all physical infrastructure
 * (Zones → Aisles → Bins) and authorized staff (Users).</p>
 */
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Warehouse extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
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
    @JsonIgnore
    private Set<Zone> zones;

    /**
     * The staff members assigned to work at this facility.
     * <p>
     * Relationship: A user is typically anchored to one warehouse to maintain
     * strict data access boundaries.
     * </p>
     */
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<User> users;
}