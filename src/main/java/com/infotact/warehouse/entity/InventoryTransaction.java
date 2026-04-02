package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_transactions")
@EntityListeners(AuditingEntityListener.class)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // e.g., INBOUND, OUTBOUND, TRANSFER, ADJUSTMENT

    @Column(nullable = false)
    private Long quantityChange; // +10 for receiving, -5 for picking

    private String referenceId; // e.g., Purchase Order ID or Sales Order ID

    private String reasonCode; // e.g., "DAMAGED_IN_TRANSIT", "STOCK_TAKE_CORRECTION"

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime transactionDate;

    @CreatedBy
    private String performedBy;
}