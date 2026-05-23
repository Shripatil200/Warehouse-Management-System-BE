package com.infotact.warehouse.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Priority tiers for the warehouse task queue.
 *
 * <p>The {@code weight} field is the numeric value used to sort tasks in the
 * {@link com.infotact.warehouse.service.TaskAssignmentEngine}.  A <b>higher weight
 * wins</b> — the engine always pops the task with the highest weight first.
 *
 * <pre>
 * Tier 1 – URGENT     (weight 300)  Urgent relocations / inventory health:
 *                                    hazardous spill, blocked emergency aisle, etc.
 *                                    Must clear the floor immediately.
 *
 * Tier 2 – HIGH       (weight 200)  Inbound shipment PUTAWAY: clear the receiving
 *                                    docks quickly to unblock the next delivery.
 *
 * Tier 3 – STANDARD   (weight 100)  Customer order PICKING / PACKING: FIFO within
 *                                    this tier (resolved by createdAt timestamp).
 * </pre>
 *
 * <p>Within the same weight the {@link com.infotact.warehouse.service.TaskAssignmentEngine}
 * uses {@code createdAt} as the tiebreaker so older tasks are served first (FIFO).
 */
@Getter
@RequiredArgsConstructor
public enum TaskPriority {

    /** Tier 1 — Urgent relocation / inventory-health issue. */
    URGENT(300),

    /** Tier 2 — Inbound shipment putaway. */
    HIGH(200),

    /** Tier 3 — Customer order picking / packing. */
    STANDARD(100);

    /**
     * Numeric weight used for queue ordering.
     * Higher value = picked first.
     */
    private final int weight;
}
