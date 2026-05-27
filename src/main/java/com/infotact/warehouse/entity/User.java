package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.OperatorStatus;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing a warehouse staff member.
 * <p>
 * All users belong to the single warehouse. The {@code warehouse_id} column
 * is non-nullable; supplier contacts are stored separately in the {@link Supplier} entity.
 * </p>
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email",     columnList = "email"),
        @Index(name = "idx_user_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_user_status",    columnList = "status")
})
public class User extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String contactNumber;

    /** BCrypt encoded password — never expose in DTOs. */
    @Column(nullable = false)
    private String password;

    /** ADMIN, MANAGER, OPERATOR, EMPLOYEE — no SUPPLIER here. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    /**
     * The warehouse this user belongs to.
     * Always non-null for warehouse staff.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * Real-time floor availability for operators.
     *
     * <p>Only meaningful when {@code role == OPERATOR}.  Other roles (ADMIN,
     * MANAGER, EMPLOYEE) are ignored by the assignment engine.
     *
     * <p>Default: {@link OperatorStatus#AVAILABLE} — existing rows get this value
     * via the Flyway migration V2__add_operator_status_to_users.sql (see below).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_status", nullable = false)
    private OperatorStatus operatorStatus = OperatorStatus.AVAILABLE;
}
