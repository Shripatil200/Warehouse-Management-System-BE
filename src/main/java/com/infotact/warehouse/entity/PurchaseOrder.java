package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.WarehouseScopedEntity;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistence entity representing a formal request for stock from a supplier.
 * <p>
 * Tracks the inbound lifecycle from PO creation through receipt.
 * The {@code supplier} field now references the dedicated {@link Supplier} entity.
 * </p>
 */
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_po_status",    columnList = "status"),
        @Index(name = "idx_po_expected",  columnList = "expectedDate")
})
public class PurchaseOrder extends WarehouseScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The supplier providing the goods.
     * References the dedicated {@link Supplier} entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private LocalDateTime orderDate;

    private LocalDateTime expectedDate;

    @Enumerated(EnumType.STRING)
    private PurchaseOrderStatus status;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL)
    private List<PurchaseOrderItem> items;
}
