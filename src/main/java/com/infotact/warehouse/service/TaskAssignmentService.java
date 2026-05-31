package com.infotact.warehouse.service;

import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.SellingOrder;
import com.infotact.warehouse.entity.PurchaseOrder;
import com.infotact.warehouse.entity.Warehouse;
import java.util.List;
import java.util.Optional;

/**
 * Core task-dispatch engine interface for the warehouse floor.
 * Handles task life cycles, real-time workload routing, and operator pooling.
 *
 * <h2>Responsibilities</h2>
 * <ol>
 * <li><b>Task Creation &amp; Routing</b> — Dynamically targets available physical human workers 
 * or places tasks inside priority queues when all operators are fully utilized.</li>
 * <li><b>Operator Release &amp; Re-dispatch</b> — Transitions completed tasks, frees 
 * the associated floor operator, and pushes the next eligible high-priority task.</li>
 * <li><b>Transactional Integrity</b> — Enforces strict serial processing bounds to protect 
 * concurrent state updates from causing dual assignments or orphaned queue slots.</li>
 * </ol>
 *
 * @see com.infotact.warehouse.service.impl.TaskAssignmentEngine
 */
public interface TaskAssignmentService {

    /**
     * Creates a new task and either assigns it immediately to a free operator
     * or parks it in the WAITING queue.
     * <p>
     * Called after the triggering entity (order/PO) has already been successfully committed 
     * to persistent storage.
     * </p>
     *
     * @param task        A fully populated but <em>unsaved</em> Task instance 
     * (status and assignedOperator should be left null).
     * @param warehouseId The warehouse scope — used to look up AVAILABLE operators.
     * @return The saved Task (status is either ASSIGNED or WAITING).
     */
    Task createAndDispatch(Task task, String warehouseId);

    /**
     * Called when an operator completes (or a manager overrides) their currently assigned task.
     * <p>
     * Implementation must execute the following operations cleanly:
     * <ol>
     * <li>Mark the target task as COMPLETED along with timestamp tracking.</li>
     * <li>Transition the user context status back to AVAILABLE.</li>
     * <li>Query and process the next highest priority waiting workflow task if one exists.</li>
     * </ol>
     * </p>
     *
     * @param completedTaskId The unique ID of the task being marked complete.
     * @param operatorId      The unique identifier of the worker submitting the completion action.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if entities are missing.
     * @throws IllegalStateException if the target task is not assigned to the invoking operator.
     */
    void completeTaskAndAssignNext(String completedTaskId, String operatorId);

    /**
     * Cancels a target task via administrative manager override action.
     * If the task was already actively ASSIGNED to a worker, that worker is 
     * immediately freed and automatically re-routed to a pending queued task.
     *
     * @param taskId      The unique ID of the task to terminate.
     * @param warehouseId The warehouse scope parameter required to bound queue lookup.
     * @throws com.infotact.warehouse.exception.ResourceNotFoundException if the task does not exist.
     * @throws IllegalStateException if the task state is already completed or canceled.
     */
    void cancelTask(String taskId, String warehouseId);

    /**
     * Builds and dispatches an active PICKING task tied to a customer fulfillment order.
     * Typically called immediately from order processing streams after structural validation.
     *
     * @param order          The active domain customer selling order entity container.
     * @param sourceLocation The human-readable warehouse physical zone/bin path targeted for picking.
     * @param warehouseId    The warehouse identity context scope.
     * @return The saved Task instance in its initial assigned or queued state.
     */
    Task createPickingTask(SellingOrder order, String sourceLocation, String warehouseId);

    /**
     * Builds and dispatches an active PUTAWAY task derived from a newly received inbound purchase order.
     * Called from inbound supply receipt components once shipment arrival records settle.
     *
     * @param purchaseOrder The purchase order entity reference containing SKU contents.
     * @param dockLocation  The specific inbound landing dock or platform identifier.
     * @param warehouseId   The warehouse identity context scope.
     * @return The saved Task instance in its initial assigned or queued state.
     */
    Task createPutawayTask(PurchaseOrder purchaseOrder, String dockLocation, String warehouseId);

    /**
     * Builds and dispatches an URGENT RELOCATION task via manual supervisor assignment.
     * Elevated priority tier designed to bypass existing standard and high priority queue elements.
     *
     * @param sourceLocation      The originating bin or sector layout path.
     * @param destinationLocation The target destination zone or cell layout path.
     * @param notes               Explicit operational descriptions or instructions for the floor operator.
     * @param warehouse           The targeted physical warehouse scope block.
     * @return The saved Task instance in its initial assigned or queued state.
     */
    Task createRelocationTask(String sourceLocation, String destinationLocation, String notes, Warehouse warehouse);

    /**
     * Returns all WAITING tasks for a specific warehouse scope sorted by priority metrics.
     * Typically drives administrative manager queues and real-time dashboard tracking panels.
     *
     * @param warehouseId The unique identification tracking sequence for the warehouse.
     * @return A list of tasks positioned within the priority queue wait state.
     */
    List<Task> getWaitingQueue(String warehouseId);

    /**
     * Returns the active IN_PROGRESS or ASSIGNED task context map tied to a specific operator.
     * Used by handheld mobile endpoints during re-authentication routines to recover active state.
     *
     * @param operatorId The operator identifier being queried.
     * @return An Optional wrapper containing the active task details if found, empty otherwise.
     */
    Optional<Task> getCurrentTaskForOperator(String operatorId);

    /**
     * Returns all pending tasks matching the operator's specialty and warehouse.
     */
    List<Task> getPendingTasksForOperator(String operatorId, String warehouseId);

    /**
     * Operator manually claims/picks a task from their pending queue.
     */
    Task claimTask(String taskId, String operatorId);

    /**
     * Operator acknowledges and starts their assigned task.
     */
    Task startTask(String taskId, String operatorId);

    /**
     * Operator puts their active task on hold.
     */
    Task holdTask(String taskId, String operatorId);
}