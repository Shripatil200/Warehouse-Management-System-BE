package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Task;
import com.infotact.warehouse.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for {@link Task}.
 *
 * <h3>Key queries</h3>
 * <ul>
 *   <li>{@link #findTopWaitingTask} — the heart of the assignment engine.
 *       Fetches the single highest-priority WAITING task using a PESSIMISTIC
 *       write lock to prevent two threads from claiming the same row.</li>
 *   <li>{@link #findByAssignedOperatorIdAndStatus} — lets an operator's mobile
 *       app retrieve their current active task on reconnect.</li>
 * </ul>
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    // ── Assignment engine query ───────────────────────────────────────────────

    /**
     * Returns the single highest-priority WAITING task for a given warehouse,
     * ordered by priority weight (DESC) then creation time (ASC — FIFO).
     *
     * Uses a native PostgreSQL query with FOR UPDATE SKIP LOCKED so that
     * concurrent threads/nodes each claim a different row rather than
     * blocking each other on the same lock. This is the DB-level guarantee
     * that makes the assignment engine cluster-safe without relying on
     * JVM-level synchronization.
     *
     * Note: SKIP LOCKED requires PostgreSQL 9.5+ or MySQL 8.0+.
     * The CASE ordering maps TaskPriority enum names to numeric weights
     * matching the TaskPriority.weight values (URGENT=300, HIGH=200, STANDARD=100).
     */
    @Query(value = """
        SELECT * FROM tasks
        WHERE warehouse_id = :warehouseId
          AND status = 'WAITING'
        ORDER BY
          CASE priority
            WHEN 'URGENT'   THEN 300
            WHEN 'HIGH'     THEN 200
            WHEN 'STANDARD' THEN 100
            ELSE 0
          END DESC,
          created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Task> findTopWaitingTask(@Param("warehouseId") String warehouseId);

    // ── Operator mobile-app queries ───────────────────────────────────────────

    /**
     * Returns the current task being worked on by an operator.
     * Used when the operator's app reconnects via SSE to rebuild UI state.
     */
    Optional<Task> findByAssignedOperatorIdAndStatus(String operatorId, TaskStatus status);

    /**
     * Returns all tasks assigned to an operator across all statuses.
     * Useful for a task-history screen in the mobile app.
     */
    List<Task> findByAssignedOperatorIdOrderByCreatedAtDesc(String operatorId);

    // ── Manager / dashboard queries ───────────────────────────────────────────

    /**
     * All currently queued (WAITING) tasks for a warehouse, in priority order.
     * Drives the manager dashboard pending-queue view.
     */
    @Query("""
       SELECT t FROM Task t \
       WHERE t.warehouse.id = :warehouseId AND t.status = com.infotact.warehouse.entity.enums.TaskStatus.WAITING \
       ORDER BY t.priority DESC, t.createdAt ASC\
       """)
    List<Task> findWaitingQueueByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Count of tasks currently in WAITING state for a warehouse.
     * Used by the dashboard summary card.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.warehouse.id = :warehouseId AND t.status = com.infotact.warehouse.entity.enums.TaskStatus.WAITING")
    long countWaitingByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Retrieves all pending tasks (status is WAITING or ON_HOLD) for a warehouse,
     * filtered by operator specialty task type if specified.
     */
    @Query("""
       SELECT t FROM Task t \
       WHERE t.warehouse.id = :warehouseId \
         AND (t.status = com.infotact.warehouse.entity.enums.TaskStatus.WAITING \
              OR t.status = com.infotact.warehouse.entity.enums.TaskStatus.ON_HOLD) \
         AND (:specialty IS NULL OR t.type = :specialty) \
       ORDER BY \
         CASE t.priority \
           WHEN com.infotact.warehouse.entity.enums.TaskPriority.URGENT   THEN 300 \
           WHEN com.infotact.warehouse.entity.enums.TaskPriority.HIGH     THEN 200 \
           WHEN com.infotact.warehouse.entity.enums.TaskPriority.STANDARD THEN 100 \
           ELSE 0 \
         END DESC, \
         t.createdAt ASC\
       """)
    List<Task> findPendingTasksByWarehouseAndSpecialty(
            @Param("warehouseId") String warehouseId,
            @Param("specialty") com.infotact.warehouse.entity.enums.TaskType specialty);

    long countByAssignedOperatorIdAndStatus(String operatorId, TaskStatus status);
}