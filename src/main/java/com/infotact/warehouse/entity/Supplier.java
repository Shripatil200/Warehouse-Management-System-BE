package com.infotact.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Persistence entity representing an external vendor or manufacturer.
 * <p>
 * Suppliers are the primary source of inbound inventory. This entity stores
 * essential contact and logistical data required to issue {@link PurchaseOrder}
 * records and coordinate stock deliveries.
 * </p>
 */
@Data
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier {

    /**
     * Unique identifier for the supplier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * The official registered business name of the vendor.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Primary email address used for procurement and automated PO notifications.
     * <p>
     * Logic: This address is typically used by the EmailService to send
     * digital copies of Purchase Orders once they are approved.
     * </p>
     */
    private String contactEmail;

    /**
     * Business phone number for logistics and dispatch coordination.
     */
    private String phone;

    /**
     * The physical or billing address of the supplier's headquarters/warehouse.
     */
    private String address;
}