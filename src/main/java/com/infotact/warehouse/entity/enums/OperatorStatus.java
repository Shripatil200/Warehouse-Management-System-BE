package com.infotact.warehouse.entity.enums;

/**
 * Tracks the real-time availability of a warehouse floor operator.
 *
 * <p>State machine:
 * <pre>
 *   AVAILABLE ──► BUSY       (task assigned via TaskAssignmentEngine)
 *   BUSY      ──► AVAILABLE  (operator calls completeTask endpoint)
 * </pre>
 * Only operators with status AVAILABLE are eligible to receive the next
 * highest-priority task from the queue.
 */
public enum OperatorStatus {

    /**
     * Operator is idle and ready to receive the next task.
     * The assignment engine will push a queued task to this operator immediately.
     */
    AVAILABLE,

    /**
     * Operator currently has exactly one task in hand.
     * Any new tasks triggered while all operators are BUSY go into
     * the PENDING_QUEUE with {@link TaskStatus#WAITING}.
     */
    BUSY
}
