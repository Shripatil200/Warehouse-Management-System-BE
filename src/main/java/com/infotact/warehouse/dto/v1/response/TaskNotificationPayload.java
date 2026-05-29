package com.infotact.warehouse.dto.v1.response;

import com.infotact.warehouse.entity.enums.TaskPriority;
import com.infotact.warehouse.entity.enums.TaskStatus;
import com.infotact.warehouse.entity.enums.TaskType;

import java.time.LocalDateTime;

/**
 * JSON payload pushed to an operator's mobile device via SSE the instant
 * a task is assigned to them.
 *
 * <p>Kept intentionally lean — the mobile app can call
 * {@code GET /api/v1/tasks/{id}} for full detail if needed.
 *
 * <p>Example JSON emitted on the SSE stream:
 * <pre>{@code
 * {
 *   "taskId":         "a3f7c2d1-...",
 *   "type":           "PICKING",
 *   "priority":       "STANDARD",
 *   "priorityWeight": 100,
 *   "sourceLocation": "Zone-A / Aisle-3 / Bin-07",
 *   "notes":          null,
 *   "assignedAt":     "2026-05-23T14:32:00"
 * }
 * }</pre>
 */
public record TaskNotificationPayload(

        /** The UUID of the task — used by the app to fetch full detail. */
        String taskId,

        /** What the operator must do physically. */
        TaskType type,

        /** Priority tier label (URGENT / HIGH / STANDARD). */
        TaskPriority priority,

        /** Numeric weight — lets the mobile UI render a priority badge. */
        int priorityWeight,

        /**
         * Where the operator should go first.
         * Human-readable bin/aisle address (e.g., "Zone-B / Aisle-1 / Bin-04").
         */
        String sourceLocation,

        /**
         * Destination bin for PUTAWAY / RELOCATION tasks; null for PICKING.
         */
        String destinationLocation,

        /** Optional special instructions or hazard warning. */
        String notes,

        /** Timestamp of assignment for display on the operator's screen. */
        LocalDateTime assignedAt,

        /** The customer order associated with this task. */
        String sourceOrderId,

        /** The supplier purchase order associated with this task. */
        String sourcePurchaseOrderId,

        /** The status of the task. */
        TaskStatus status
) {
    /**
     * Factory method — builds the payload directly from a persisted task.
     */
    public static TaskNotificationPayload from(com.infotact.warehouse.entity.Task task) {
        return new TaskNotificationPayload(
                task.getId(),
                task.getType(),
                task.getPriority(),
                task.getPriority().getWeight(),
                task.getSourceLocation(),
                task.getDestinationLocation(),
                task.getNotes(),
                task.getAssignedAt(),
                task.getSourceOrder() != null ? task.getSourceOrder().getId() : null,
                task.getSourcePurchaseOrder() != null ? task.getSourcePurchaseOrder().getId() : null,
                task.getStatus()
        );
    }
}
