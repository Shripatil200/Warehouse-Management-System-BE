package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.dto.v1.response.TaskNotificationPayload;
import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.event.TaskCreatedEvent;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.OperatorSseRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Listens for {@link TaskCreatedEvent} and broadcasts SSE notification
 * to active warehouse operators specialized in the task's type after the database transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCreatedEventListener {

    private final OperatorSseRegistryService sseRegistry;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        Task task = event.getTask();
        String warehouseId = task.getWarehouse().getId();
        TaskNotificationPayload payload = TaskNotificationPayload.from(task);

        log.info("[Listener] Broadcasting TASK_CREATED notification for task {} in warehouse {}",
                task.getId(), warehouseId);

        // Find all ACTIVE operators in this warehouse
        List<User> operators = userRepository.findByWarehouseIdAndRoleAndStatus(
                warehouseId, Role.OPERATOR, UserStatus.ACTIVE);

        for (User operator : operators) {
            // Only send to operators who have no specialty restriction OR whose specialty matches the task type
            if (operator.getSpecialty() == null || operator.getSpecialty() == task.getType()) {
                log.debug("[Listener] Sending TASK_CREATED to operator {}", operator.getEmail());
                sseRegistry.sendToOperator(operator.getId(), "TASK_CREATED", payload);
            }
        }
    }
}
