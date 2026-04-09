package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing a registered system user.
 * <p>
 * This entity is the core of the security and multi-tenant architecture.
 * Every user is anchored to a specific {@link Warehouse}, ensuring data
 * isolation where staff can only view and manage stock within their assigned facility.
 * </p>
 */
@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_user_status", columnList = "status")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Display name of the user (e.g., John Doe).
     */
    @Column(nullable = false)
    private String name;

    /**
     * Unique login identifier and primary communication channel.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Unique contact number. Used for SMS notifications and identification.
     */
    @Column(unique = true, nullable = false)
    private String contactNumber;

    /**
     * BCrypt encoded password hash.
     * <p>
     * Security: This field should never be exposed in DTOs. It is used
     * exclusively by the AuthenticationProvider during the login flow.
     * </p>
     */
    @Column(nullable = false)
    private String password;

    /**
     * Authorization level (e.g., ADMIN, MANAGER, EMPLOYEE).
     * <p>
     * Logic: Determines access to specific API endpoints and dashboard features
     * via Spring Security Method Security.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * Account lifecycle state (e.g., ACTIVE, INACTIVE).
     * <p>
     * Logic: Users with 'INACTIVE' status are blocked from authenticating
     * even if their credentials are correct.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    /**
     * The physical facility this user is authorized to manage.
     * <p>
     * Isolation: This relationship is used to filter all inventory, orders,
     * and layout queries based on the logged-in user's context.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
}