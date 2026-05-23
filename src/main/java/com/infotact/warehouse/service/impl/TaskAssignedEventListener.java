package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.TaskNotificationPayload;
import com.infotact.warehouse.event.TaskAssignedEvent;
import com.infotact.warehouse.service.OperatorSseRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link TaskAssignedEvent} and pushes the SSE notification to the
 * operator's device AFTER the database transaction commits.
 *
 * <h2>Why AFTER_COMMIT?</h2>
 * <p>Using {@link TransactionPhase#AFTER_COMMIT} ensures that:
 * <ol>
 *   <li>The task row is fully visible in the DB before the mobile app receives
 *       the push — so an immediate {@code GET /tasks/{id}} from the app will
 *       always find the record.</li>
 *   <li>A failed SSE push never triggers a transaction rollback — persistence is
 *       durable regardless of the operator's connectivity.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssignedEventListener {

    private final OperatorSseRegistryService sseRegistry;

    /**
     * Receives the event and forwards the notification to the operator's SSE stream.
     *
     * <p>The SSE event name is {@code "TASK_ASSIGNED"} — the React Native app
     * listens for this event name on its {@code EventSource} instance and renders
     * the task details.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAssigned(TaskAssignedEvent event) {
        String operatorId = event.getOperator().getId();
        TaskNotificationPayload payload = TaskNotificationPayload.from(event.getTask());

        log.info("[Listener] Pushing TASK_ASSIGNED notification to operator {} for task {}",
                operatorId, event.getTask().getId());

        sseRegistry.sendToOperator(operatorId, "TASK_ASSIGNED", payload);
    }
}
