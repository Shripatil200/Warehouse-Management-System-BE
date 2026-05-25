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
 * Suppliers are managed by warehouse Admins and Managers.
 * They are referenced by Purchase Orders, Consignment Agreements, and Bin Rentals.
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

    /** Business email — contact email for the supplier. */
    @Column(unique = true, nullable = false)
    private String email;

    /** 10-digit primary mobile number. */
    @Column(unique = true, nullable = false)
    private String contactNumber;

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
