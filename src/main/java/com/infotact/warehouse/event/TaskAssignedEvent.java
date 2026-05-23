package com.infotact.warehouse.event;

import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.User;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published by {@link com.infotact.warehouse.service.TaskAssignmentEngine}
 * immediately after a task is assigned to an operator and persisted.
 *
 * <p>The SSE notification controller listens for this event via
 * {@link org.springframework.context.event.EventListener} and pushes the
 * payload to the operator's open SSE connection.  Decoupling the persistence
 * transaction from the SSE push ensures a failed push never rolls back the
 * database write.
 *
 * <p>Use {@code @TransactionalEventListener(phase = AFTER_COMMIT)} in the
 * listener so the push only fires after the DB transaction is committed and
 * the task row is visible to all connections.
 */
public class TaskAssignedEvent extends ApplicationEvent {

    /** The fully persisted, assigned task. */
    private final Task task;

    /** The operator the task was assigned to. */
    private final User operator;

    public TaskAssignedEvent(Object source, Task task, User operator) {
        super(source);
        this.task     = task;
        this.operator = operator;
    }

    public Task getTask()         { return task; }
    public User getOperator()     { return operator; }
}
