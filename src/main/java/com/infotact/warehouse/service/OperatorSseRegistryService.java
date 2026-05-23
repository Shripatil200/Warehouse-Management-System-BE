package com.infotact.warehouse.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service interface managing an in-memory registry of active Server-Sent Events (SSE)
 * connections keyed by warehouse operator user-ID.
 *
 * <h2>Why one emitter per operator?</h2>
 * <p>Each floor operator utilizes a React Native mobile application that establishes exactly
 * one persistent SSE downstream connection upon a successful authentication/login lifecycle.
 * By keying the registry directly by {@code operatorId}, dispatch and assignment engine threads
 * can push targeted payloads immediately to specific physical devices without resorting to
 * expensive system-wide broadcasting.
 * </p>
 *
 * <h2>Thread Safety Expectations</h2>
 * <p>Implementations must guarantee thread-safe read, write, and removal behavior to accommodate
 * concurrent registration updates, timeouts, framework-driven cleanups, and task engine dispatch routines.
 * </p>
 *
 * <h2>Cluster Architecture Note</h2>
 * <p>Standard implementations of this interface are restricted to single-JVM scopes.
 * For multi-node horizontally scaled cloud topologies, it is recommended that this abstraction layer
 * be backed by a Redis Pub/Sub message broker or similar data grid strategy:
 * <ol>
 * <li>Every localized application worker node subscribes to a unique Redis channel pattern keyed by {@code operatorId}.</li>
 * <li>The task dispatcher publishes updates to the cluster broker; whichever node owns the real physical SSE socket
 * connection intercepts the event and forwards the stream frame to the remote device client.</li>
 * </ol>
 * </p>
 *
 * @see com.infotact.warehouse.service.impl.OperatorSseRegistryImpl
 */
public interface OperatorSseRegistryService {

    /**
     * Registers a new persistent Server-Sent Events downstream channel stream for a specified operator.
     * <p>
     * If an operator initiates a reconnection loop (e.g., application transitioning from background
     * sleep state to active foreground execution), any existing registered connection mapping for that
     * target worker context must be cleanly superseded and detached.
     * </p>
     *
     * @param operatorId The unique identification profile tracking sequence of the connecting operator.
     * @param timeout    The concrete downstream channel inactivity timeout boundary value in milliseconds
     * (use a value of 0 to indicate indefinite session survival thresholds).
     * @return A fully initialized, thread-safe {@link SseEmitter} handle bound into monitoring triggers.
     */
    SseEmitter register(String operatorId, long timeout);

    /**
     * Pushes a structured event stream object block to a targeted active warehouse operator context channel.
     * <p>
     * This operation must fail silently or gracefully no-op if the requested operator does not possess a live
     * connection registry pointer mapping (e.g., application layer drop, cellular blackout, offline).
     * Because underlying workflows are safely committed to persistence blocks, workers will implicitly synchronized
     * state gaps on re-polling current profiles.
     * </p>
     *
     * @param operatorId The unique target operator identification profile sequence.
     * @param eventName  The clear descriptive SSE frame routing marker name (e.g., {@code "TASK_ASSIGNED"}).
     * @param payload    The domain data context object entity targeted for serialization to JSON syntax layout.
     */
    void sendToOperator(String operatorId, String eventName, Object payload);

    /**
     * Inspects if a target operator currently holds a registered active streaming channel resource node.
     * Typically utilized to drive manager tracking monitors, health metrics, and device visibility layers.
     *
     * @param operatorId The unique operator identity context block sequence.
     * @return true if an active communication pipeline is pinned in memory, false otherwise.
     */
    boolean isConnected(String operatorId);

    /**
     * Computes the current structural scale metric tracking active network allocations inside the container map.
     * Used for dashboard analytics and connection load distribution auditing.
     *
     * @return The aggregated integer sum of live managed operator sockets.
     */
    int connectionCount();
}