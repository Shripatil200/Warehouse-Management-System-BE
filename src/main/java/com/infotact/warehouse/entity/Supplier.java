package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing an independent supplier.
 * <p>
 * Suppliers are global entities — they are NOT scoped to any warehouse.
 * They register themselves, manage their own profile and product catalogue,
 * and are visible to all warehouses when raising Purchase Orders.
 * </p>
 * <p>
 * Authentication: Suppliers use their own {@code email}/{@code password} to log in.
 * The {@link com.infotact.warehouse.config.JWT.SupplierDetailsService} loads them
 * separately from warehouse {@link User} accounts so the two tables never mix.
 * </p>
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "suppliers",
        indexes = {
                @Index(name = "idx_supplier_email",  columnList = "email"),
                @Index(name = "idx_supplier_status", columnList = "status")
        }
)
public class Supplier extends BaseEntity {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Contact person full name. */
    @Column(nullable = false)
    private String name;

    /** Business email — used as login username. */
    @Column(unique = true, nullable = false)
    private String email;

    /** 10-digit primary mobile number. */
    @Column(unique = true, nullable = false)
    private String contactNumber;

    /** BCrypt encoded password — never expose in DTOs. */
    @Column(nullable = false)
    private String password;

    /** Registered business / company name. */
    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    /** GST or tax registration number (optional). */
    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    /** Physical or billing address (optional). */
    @Column(name = "address", length = 500)
    private String address;

    /** Company website URL (optional). */
    @Column(name = "website", length = 255)
    private String website;

    /** Account lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status = SupplierStatus.ACTIVE;
}
