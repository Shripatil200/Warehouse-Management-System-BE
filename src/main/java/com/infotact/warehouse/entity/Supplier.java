package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import com.infotact.warehouse.entity.enums.SupplierStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing a supplier linked to this warehouse.
 * <p>
 * Extends {@link WarehouseScopedEntity} so that every supplier record is
 * anchored to the warehouse that registered them. This prevents cross-deployment
 * data leakage in case two instances ever share a database (they shouldn't, but
 * the FK makes the schema self-documenting and safe).
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
                @Index(name = "idx_supplier_email",     columnList = "email"),
                @Index(name = "idx_supplier_status",    columnList = "status"),
                @Index(name = "idx_supplier_warehouse", columnList = "warehouse_id")
        }
)
public class Supplier extends WarehouseScopedEntity {

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
