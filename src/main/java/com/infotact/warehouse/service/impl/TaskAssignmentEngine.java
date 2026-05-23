package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.OperatorStatus;
import com.infotact.warehouse.entity.enums.TaskPriority;
import com.infotact.warehouse.entity.enums.TaskStatus;
import com.infotact.warehouse.entity.enums.TaskType;
import com.infotact.warehouse.event.TaskAssignedEvent;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.TaskRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.TaskAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core task-dispatch engine for the warehouse floor.
 *
 * <h2>Responsibilities</h2>
 * <ol>
 *   <li><b>Task creation</b> — called by {@link OrderServiceImpl} (PICKING/PACKING)
 *       and {@link PurchaseOrderServiceImpl} (PUTAWAY) after their own commits,
 *       and by the manual relocation API (RELOCATION).</li>
 *   <li><b>Assignment</b> — decides whether to assign immediately or queue.</li>
 *   <li><b>Completion callback</b> — when an operator finishes, flips their status
 *       back to AVAILABLE and immediately pulls the next WAITING task for them.</li>
 * </ol>
 *
 * <h2>Concurrency model</h2>
 * <pre>
 *  JVM-level:  {@code synchronized} on {@code assignNextTaskToOperator} ensures that
 *              if two operators finish at the exact same millisecond, their threads
 *              are serialised and both get different tasks (no double-assignment).
 *
 *  DB-level:   {@code PESSIMISTIC_WRITE + SKIP LOCKED} in TaskRepository ensures
 *              that in a multi-instance (clustered) deployment, only one JVM node
 *              locks a given WAITING task row at a time.
 * </pre>
 *
 * <p>Together these provide belt-and-suspenders safety for both single-instance
 * and horizontally-scaled deployments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAssignmentEngine implements TaskAssignmentService {

    private final TaskRepository         taskRepository;
    private final UserRepository         userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── 1.  Task Creation ─────────────────────────────────────────────────────

    /**
     * Creates a new task and either assigns it immediately to a free operator
     * or parks it in the WAITING queue.
     *
     * <p>Called after the triggering entity (order/PO) has already been persisted.
     *
     * @param task        A fully populated but <em>unsaved</em> Task instance
     *                    (status and assignedOperator should be left null — this
     *                    method sets them).
     * @param warehouseId The warehouse scope — used to find AVAILABLE operators.
     * @return The saved Task (status is either ASSIGNED or WAITING).
     */
    @Transactional
    public Task createAndDispatch(Task task, String warehouseId) {
        // Find the first AVAILABLE operator in this warehouse (any order — engine
        // will re-balance via the priority queue when they complete tasks).
        Optional<User> availableOperator = userRepository
                .findFirstByWarehouseIdAndOperatorStatus(warehouseId, OperatorStatus.AVAILABLE);

        if (availableOperator.isPresent()) {
            User operator = availableOperator.get();
            assignTaskToOperator(task, operator);
            Task saved = taskRepository.save(task);
            log.info("[TaskEngine] Task {} ({}/{}) assigned directly to operator {}",
                    saved.getId(), saved.getType(), saved.getPriority(), operator.getEmail());
            publishAssignmentEvent(saved, operator);
            return saved;
        }

        // All operators are BUSY — park in queue
        task.setStatus(TaskStatus.WAITING);
        Task saved = taskRepository.save(task);
        log.info("[TaskEngine] Task {} ({}/{}) queued — all operators BUSY. Queue depth: {}",
                saved.getId(), saved.getType(), saved.getPriority(),
                taskRepository.countWaitingByWarehouse(warehouseId));
        return saved;
    }

    // ── 2.  Assignment on Operator Completion ─────────────────────────────────

    /**
     * Called when an operator completes (or a manager cancels) their current task.
     *
     * <p>Steps:
     * <ol>
     *   <li>Marks the operator's completed task as {@link TaskStatus#COMPLETED}.</li>
     *   <li>Sets the operator's status back to {@link OperatorStatus#AVAILABLE}.</li>
     *   <li>Immediately fetches the highest-priority WAITING task (if any) and
     *       assigns it to them.</li>
     * </ol>
     *
     * <p>The {@code synchronized} keyword serialises concurrent completions at the
     * JVM level.  Combined with {@code PESSIMISTIC_WRITE} in the repository query,
     * this prevents double-assignment in clustered environments.
     *
     * @param completedTaskId The ID of the task the operator just finished.
     * @param operatorId      The ID of the operator who completed it.
     */
    @Transactional
    public synchronized void completeTaskAndAssignNext(String completedTaskId, String operatorId) {

        // ── a. Resolve entities ──────────────────────────────────────────────
        Task completedTask = taskRepository.findById(completedTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: " + completedTaskId));

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Operator not found: " + operatorId));

        // ── b. Guard: only the assigned operator or a manager should call this ─
        if (completedTask.getAssignedOperator() == null ||
                !completedTask.getAssignedOperator().getId().equals(operatorId)) {
            throw new IllegalStateException(
                    "Task " + completedTaskId + " is not assigned to operator " + operatorId);
        }

        // ── c. Mark task complete ────────────────────────────────────────────
        completedTask.setStatus(TaskStatus.COMPLETED);
        completedTask.setCompletedAt(LocalDateTime.now());
        taskRepository.save(completedTask);
        log.info("[TaskEngine] Task {} COMPLETED by operator {}", completedTaskId, operator.getEmail());

        // ── d. Free the operator ─────────────────────────────────────────────
        operator.setOperatorStatus(OperatorStatus.AVAILABLE);
        userRepository.save(operator);

        // ── e. Pull next highest-priority waiting task ───────────────────────
        assignNextTaskToOperator(operator);
    }

    /**
     * Cancels a task (manager action).  If the task was ASSIGNED, the operator
     * is freed and the next queued task is dispatched to them.
     *
     * @param taskId     The task to cancel.
     * @param warehouseId Required to scope the queue lookup.
     */
    @Transactional
    public synchronized void cancelTask(String taskId, String warehouseId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel a task in status: " + task.getStatus());
        }

        User previousOperator = task.getAssignedOperator();
        task.setStatus(TaskStatus.CANCELLED);
        taskRepository.save(task);
        log.info("[TaskEngine] Task {} CANCELLED", taskId);

        if (previousOperator != null) {
            previousOperator.setOperatorStatus(OperatorStatus.AVAILABLE);
            userRepository.save(previousOperator);
            assignNextTaskToOperator(previousOperator);
        }
    }

    // ── 3.  Convenience factory helpers (used by service layer) ──────────────

    /**
     * Builds and dispatches a PICKING task from a customer order.
     * Called from {@code OrderServiceImpl.createOrder()} after the order is persisted.
     */
    @Transactional
    public Task createPickingTask(
            com.infotact.warehouse.entity.SellingOrder order,
            String sourceLocation,
            String warehouseId) {

        Task task = new Task();
        task.setType(TaskType.PICKING);
        task.setPriority(TaskPriority.STANDARD);
        task.setSourceOrder(order);
        task.setSourceLocation(sourceLocation);
        task.setWarehouse(order.getWarehouse());
        task.setNotes("Pick items for order: " + order.getOrderNumber());

        return createAndDispatch(task, warehouseId);
    }

    /**
     * Builds and dispatches a PUTAWAY task from a received purchase order.
     * Called from {@code PurchaseOrderServiceImpl} when a shipment is received.
     */
    @Transactional
    public Task createPutawayTask(
            com.infotact.warehouse.entity.PurchaseOrder purchaseOrder,
            String dockLocation,
            String warehouseId) {

        Task task = new Task();
        task.setType(TaskType.PUTAWAY);
        task.setPriority(TaskPriority.HIGH);
        task.setSourcePurchaseOrder(purchaseOrder);
        task.setSourceLocation(dockLocation);
        task.setWarehouse(purchaseOrder.getWarehouse());
        task.setNotes("Putaway inbound shipment from supplier: "
                + purchaseOrder.getSupplier().getName());

        return createAndDispatch(task, warehouseId);
    }

    /**
     * Builds and dispatches an URGENT RELOCATION task (manager-initiated).
     * Highest-priority tier — will jump all other queued tasks.
     */
    @Transactional
    public Task createRelocationTask(
            String sourceLocation,
            String destinationLocation,
            String notes,
            com.infotact.warehouse.entity.Warehouse warehouse) {

        Task task = new Task();
        task.setType(TaskType.RELOCATION);
        task.setPriority(TaskPriority.URGENT);
        task.setSourceLocation(sourceLocation);
        task.setDestinationLocation(destinationLocation);
        task.setWarehouse(warehouse);
        task.setNotes(notes);

        return createAndDispatch(task, warehouse.getId());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Fetches the top WAITING task for the operator's warehouse and assigns it.
     * No-op if the queue is empty (operator stays AVAILABLE, ready for next trigger).
     *
     * <p>This method is intentionally {@code synchronized} to ensure that concurrent
     * calls (e.g., two operators completing tasks simultaneously) serialize their
     * queue pops and each gets a unique task.
     */
    private synchronized void assignNextTaskToOperator(User operator) {
        String warehouseId = operator.getWarehouse().getId();

        taskRepository.findTopWaitingTask(warehouseId).ifPresentOrElse(
                nextTask -> {
                    assignTaskToOperator(nextTask, operator);
                    taskRepository.save(nextTask);
                    log.info("[TaskEngine] Task {} ({}/{}) auto-assigned to operator {} from queue",
                            nextTask.getId(), nextTask.getType(), nextTask.getPriority(),
                            operator.getEmail());
                    publishAssignmentEvent(nextTask, operator);
                },
                () -> log.debug("[TaskEngine] Queue empty for warehouse {}. Operator {} is AVAILABLE.",
                        warehouseId, operator.getEmail())
        );
    }

    /** Mutates task + operator in memory (caller is responsible for saving). */
    private void assignTaskToOperator(Task task, User operator) {
        task.setStatus(TaskStatus.ASSIGNED);
        task.setAssignedOperator(operator);
        task.setAssignedAt(LocalDateTime.now());

        operator.setOperatorStatus(OperatorStatus.BUSY);
        userRepository.save(operator);
    }

    /** Publishes the application event AFTER the transaction commits. */
    private void publishAssignmentEvent(Task task, User operator) {
        eventPublisher.publishEvent(new TaskAssignedEvent(this, task, operator));
    }

    // ── Queries for controllers ───────────────────────────────────────────────

    /**
     * Returns all WAITING tasks for the warehouse, in priority order.
     * Drives the manager "Pending Queue" dashboard panel.
     */
    @Transactional(readOnly = true)
    public List<Task> getWaitingQueue(String warehouseId) {
        return taskRepository.findWaitingQueueByWarehouse(warehouseId);
    }

    /**
     * Returns the current IN_PROGRESS or ASSIGNED task for an operator.
     * Called by the mobile app on reconnect to restore UI state.
     */
    @Transactional(readOnly = true)
    public Optional<Task> getCurrentTaskForOperator(String operatorId) {
        Optional<Task> assigned = taskRepository.findByAssignedOperatorIdAndStatus(
                operatorId, TaskStatus.ASSIGNED);
        if (assigned.isPresent()) return assigned;
        return taskRepository.findByAssignedOperatorIdAndStatus(
                operatorId, TaskStatus.IN_PROGRESS);
    }
}
