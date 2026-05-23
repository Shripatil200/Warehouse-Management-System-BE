package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.TenantAwareEntity;
import com.infotact.warehouse.entity.enums.TaskPriority;
import com.infotact.warehouse.entity.enums.TaskStatus;
import com.infotact.warehouse.entity.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * Core task entity — the single unit of work dispatched to a floor operator.
 *
 * <h3>Relationships</h3>
 * <ul>
 *   <li>{@code assignedOperator} — nullable until the assignment engine picks an
 *       AVAILABLE operator. Becomes non-null the moment the task leaves WAITING.</li>
 *   <li>{@code sourceOrder}      — populated for PICKING / PACKING tasks.</li>
 *   <li>{@code sourcePurchaseOrder} — populated for PUTAWAY tasks.</li>
 *   <li>Both source FKs are nullable because RELOCATION tasks have neither.</li>
 * </ul>
 *
 * <h3>Queue Ordering</h3>
 * The assignment engine orders the queue by:
 * <ol>
 *   <li>{@code priority.weight} DESC (URGENT=300 &gt; HIGH=200 &gt; STANDARD=100)</li>
 *   <li>{@code createdAt} ASC (FIFO tiebreaker within the same tier)</li>
 * </ol>
 *
 * <h3>Concurrency</h3>
 * {@code @Version} provides optimistic locking so two threads cannot simultaneously
 * assign the same task to two different operators.
 */
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_task_status",            columnList = "status"),
        @Index(name = "idx_task_operator",          columnList = "assigned_operator_id"),
        @Index(name = "idx_task_priority_created",  columnList = "priority, created_at"),
        @Index(name = "idx_task_warehouse",         columnList = "warehouse_id"),
        @Index(name = "idx_task_source_order",      columnList = "source_order_id"),
        @Index(name = "idx_task_source_po",         columnList = "source_purchase_order_id")
    }
)
public class Task extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ── Optimistic locking ───────────────────────────────────────────────────
    /**
     * JPA optimistic-lock version field.
     * Prevents two concurrent transactions from double-assigning the same task.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    // ── Classification ───────────────────────────────────────────────────────

    /**
     * What the operator must physically do (PICKING, PUTAWAY, RELOCATION, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    /**
     * Queue weight tier. Drives the priority-ordered fetch in the assignment engine.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    /**
     * Current lifecycle state.
     * Transitions: WAITING → ASSIGNED → IN_PROGRESS → COMPLETED | CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    // ── Source document links ────────────────────────────────────────────────

    /**
     * The customer order that triggered this PICKING / PACKING task.
     * Null for PUTAWAY and RELOCATION tasks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_order_id")
    private SellingOrder sourceOrder;

    /**
     * The purchase order / inbound shipment that triggered this PUTAWAY task.
     * Null for order-based and relocation tasks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_purchase_order_id")
    private PurchaseOrder sourcePurchaseOrder;

    // ── Location guidance ────────────────────────────────────────────────────

    /**
     * Human-readable source location given to the operator.
     * For PICKING: the bin address where stock is reserved (e.g., "Zone-A / Aisle-3 / Bin-07").
     * For PUTAWAY: the receiving dock reference.
     * For RELOCATION: the origin bin.
     */
    @Column(length = 255)
    private String sourceLocation;

    /**
     * Destination bin for PUTAWAY and RELOCATION tasks.
     * Not applicable for PICKING / PACKING.
     */
    @Column(length = 255)
    private String destinationLocation;

    /**
     * Free-text description or special instructions for the operator.
     * Used especially for URGENT relocations (e.g., "Hazardous spill in Zone-B row 4").
     */
    @Column(length = 1000)
    private String notes;

    // ── Assignment ───────────────────────────────────────────────────────────

    /**
     * The floor operator currently responsible for this task.
     * Null while status = WAITING (no operator assigned yet).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private User assignedOperator;

    /**
     * Timestamp when this task was assigned to {@code assignedOperator}.
     */
    private LocalDateTime assignedAt;

    /**
     * Timestamp when the operator called completeTask() on this record.
     */
    private LocalDateTime completedAt;
}
