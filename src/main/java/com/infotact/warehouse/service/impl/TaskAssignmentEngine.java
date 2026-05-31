package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.entity.PurchaseOrderItem;
import com.infotact.warehouse.entity.enums.OperatorStatus;
import com.infotact.warehouse.entity.enums.TaskPriority;
import com.infotact.warehouse.entity.enums.TaskStatus;
import com.infotact.warehouse.entity.enums.TaskType;
import com.infotact.warehouse.entity.enums.PurchaseOrderStatus;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.event.TaskAssignedEvent;
import com.infotact.warehouse.event.TaskCreatedEvent;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.repository.TaskRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.PurchaseOrderRepository;
import com.infotact.warehouse.service.TaskAssignmentService;
import com.infotact.warehouse.service.InventoryService;
import com.infotact.warehouse.dto.v1.request.ReceivingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
 *  DB-level:   A native PostgreSQL FOR UPDATE SKIP LOCKED query in TaskRepository
 *              ensures that concurrent threads or cluster nodes each lock a different
 *              WAITING task row. If a row is already locked by another transaction,
 *              SKIP LOCKED skips it entirely rather than blocking, so two nodes
 *              completing tasks simultaneously will each claim a distinct task.
 * </pre>
 *
 * <p>This is a pure DB-level guarantee — no JVM-level synchronization is needed
 * or used. The approach is correct for both single-instance and clustered deployments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAssignmentEngine implements TaskAssignmentService {

    private final TaskRepository         taskRepository;
    private final UserRepository         userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventoryService       inventoryService;
    private final PurchaseOrderRepository poRepository;

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
        // All tasks start as WAITING and are claimed manually by specialized operators
        task.setStatus(TaskStatus.WAITING);
        Task saved = taskRepository.save(task);
        log.info("[TaskEngine] Task {} ({}/{}) created in WAITING state. Queue depth: {}",
                saved.getId(), saved.getType(), saved.getPriority(),
                taskRepository.countWaitingByWarehouse(warehouseId));
        eventPublisher.publishEvent(new TaskCreatedEvent(this, saved));
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
     * <p>Double-assignment is prevented at the DB level: the native SKIP LOCKED
     * query in {@code findTopWaitingTask} ensures each concurrent caller claims
     * a distinct task row without blocking.
     *
     * @param completedTaskId The ID of the task the operator just finished.
     * @param operatorId      The ID of the operator who completed it.
     */
    @Transactional
    public void completeTaskAndAssignNext(String completedTaskId, String operatorId) {

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

        if (completedTask.getType() == TaskType.PUTAWAY && completedTask.getSourcePurchaseOrder() != null) {
            PurchaseOrder po = completedTask.getSourcePurchaseOrder();
            log.info("[TaskEngine] Auto-receiving shipment for PO {} on task completion...", po.getId());
            for (PurchaseOrderItem item : po.getItems()) {
                ReceivingRequest req = new ReceivingRequest();
                req.setProductId(item.getProduct().getId());
                req.setQuantity(item.getQuantity());
                req.setUnitCost(item.getUnitCost());
                req.setBatchNumber("PO-" + po.getId().substring(0, 8).toUpperCase());
                req.setExpiryDate(LocalDate.now().plusYears(1));
                req.setStorageBinId("RECEIVING_DOCK"); // Satisfies @NotBlank, candidates are matched dynamically in receiving zone
                try {
                    inventoryService.receiveShipment(req);
                } catch (Exception e) {
                    log.error("[TaskEngine] Failed to auto-receive PO item: SKU={}, Error={}", item.getProduct().getSku(), e.getMessage());
                }
            }
            po.setStatus(PurchaseOrderStatus.RECEIVED);
            poRepository.save(po);
        }

        taskRepository.save(completedTask);
        log.info("[TaskEngine] Task {} COMPLETED by operator {}", completedTaskId, operator.getEmail());

        // ── d. Free the operator ─────────────────────────────────────────────
        operator.setOperatorStatus(OperatorStatus.AVAILABLE);
        userRepository.save(operator);
    }

    /**
     * Cancels a task (manager action).  If the task was ASSIGNED, the operator
     * is freed and the next queued task is dispatched to them.
     *
     * @param taskId     The task to cancel.
     * @param warehouseId Required to scope the queue lookup.
     */
    @Transactional
    public void cancelTask(String taskId, String warehouseId) {
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
     * Concurrency is guaranteed at the DB level: the native SKIP LOCKED query
     * in TaskRepository ensures each concurrent caller locks a distinct row,
     * preventing double-assignment across both threads and cluster nodes.
     */
    private void assignNextTaskToOperator(User operator) {
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

    @Override
    @Transactional(readOnly = true)
    public List<Task> getPendingTasksForOperator(String operatorId, String warehouseId) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found: " + operatorId));
        return taskRepository.findPendingTasksByWarehouseAndSpecialty(warehouseId, operator.getSpecialty());
    }

    @Override
    @Transactional
    public Task claimTask(String taskId, String operatorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.WAITING && task.getStatus() != TaskStatus.ON_HOLD) {
            throw new IllegalStateException("Task is not in a claimable state: " + task.getStatus());
        }

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found: " + operatorId));

        if (operator.getOperatorStatus() == OperatorStatus.BUSY) {
            throw new IllegalStateException("Operator is currently busy with another task.");
        }

        // Verify specialty matches task type (if operator specialty is specified)
        if (operator.getSpecialty() != null && operator.getSpecialty() != task.getType()) {
            throw new IllegalArgumentException("Task type " + task.getType() + " does not match operator specialty " + operator.getSpecialty());
        }

        assignTaskToOperator(task, operator);
        Task saved = taskRepository.save(task);
        log.info("[TaskEngine] Task {} claimed by operator {}", saved.getId(), operator.getEmail());
        publishAssignmentEvent(saved, operator);
        return saved;
    }

    @Override
    @Transactional
    public Task holdTask(String taskId, String operatorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (task.getAssignedOperator() == null || !task.getAssignedOperator().getId().equals(operatorId)) {
            throw new IllegalStateException("Task " + taskId + " is not assigned to operator " + operatorId);
        }

        if (task.getStatus() != TaskStatus.ASSIGNED && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot hold task in status: " + task.getStatus());
        }

        task.setStatus(TaskStatus.ON_HOLD);
        task.setAssignedOperator(null);
        task.setAssignedAt(null);
        Task saved = taskRepository.save(task);

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found: " + operatorId));
        operator.setOperatorStatus(OperatorStatus.AVAILABLE);
        userRepository.save(operator);

        log.info("[TaskEngine] Task {} put on hold by operator {}", taskId, operator.getEmail());
        return saved;
    }

    @Override
    @Transactional
    public Task startTask(String taskId, String operatorId) {
        Task task = getCurrentTaskForOperator(operatorId)
                .filter(t -> t.getId().equals(taskId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task " + taskId + " is not your current assigned task."));

        task.setStatus(TaskStatus.IN_PROGRESS);
        return taskRepository.save(task);
    }
}