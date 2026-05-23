package com.infotact.warehouse.entity.enums;

/**
 * Lifecycle states for a warehouse floor {@link com.infotact.warehouse.entity.Task}.
 *
 * <p>Full state machine:
 * <pre>
 *   [Trigger Event]
 *        │
 *        ▼
 *     WAITING  ──── operator becomes AVAILABLE ───► ASSIGNED
 *        │                                              │
 *        │  (skips WAITING if operator free at         ▼
 *        │   creation time)                        IN_PROGRESS
 *                                                      │
 *                                    ┌─────────────────┤
 *                                    │                 │
 *                                    ▼                 ▼
 *                               COMPLETED          CANCELLED
 * </pre>
 */
public enum TaskStatus {

    /**
     * Task created but all operators are BUSY.
     * Sits in the priority queue until an operator completes their current task.
     */
    WAITING,

    /**
     * Popped from the priority queue and assigned to a specific operator.
     * The operator's status is now {@link OperatorStatus#BUSY}.
     */
    ASSIGNED,

    /**
     * Operator has acknowledged the task on their mobile device and
     * begun physical execution (walking to the bin, picking items, etc.).
     */
    IN_PROGRESS,

    /**
     * Operator marked the task as done. Their status flips back to
     * {@link OperatorStatus#AVAILABLE} and the engine immediately pulls
     * the next WAITING task from the queue for them.
     */
    COMPLETED,

    /**
     * Task was voided — typically by a MANAGER or ADMIN — before execution.
     * The assigned operator (if any) is freed back to AVAILABLE.
     */
    CANCELLED
}
