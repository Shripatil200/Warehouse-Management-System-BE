package com.infotact.warehouse.event;

import com.infotact.warehouse.entity.Task;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published by {@link com.infotact.warehouse.service.impl.TaskAssignmentEngine}
 * immediately after a task is created in the WAITING state.
 */
public class TaskCreatedEvent extends ApplicationEvent {

    private final Task task;

    public TaskCreatedEvent(Object source, Task task) {
        super(source);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
