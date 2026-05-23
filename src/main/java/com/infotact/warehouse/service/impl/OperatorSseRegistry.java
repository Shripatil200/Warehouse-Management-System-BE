package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.service.OperatorSseRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of active SSE connections keyed by operator user-ID.
 *
 * <h2>Why one emitter per operator?</h2>
 * <p>Each floor operator uses a React Native mobile app that opens exactly one
 * SSE connection on login.  We key the registry by {@code userId} so the
 * assignment engine can push directly to the right device without broadcasting
 * to everyone.
 *
 * <h2>Thread safety</h2>
 * <p>{@link ConcurrentHashMap} handles concurrent register/remove calls safely.
 * The {@link SseEmitter} itself is thread-safe for {@code send()} calls.
 *
 * <h2>Cluster note</h2>
 * <p>This implementation is single-JVM only.  For a horizontally-scaled
 * deployment replace this with a Redis Pub/Sub fan-out:
 * <ol>
 *   <li>Each node subscribes to a channel keyed by {@code operatorId}.</li>
 *   <li>The assignment engine publishes to Redis; the node holding that
 *       operator's SSE connection receives the message and forwards it.</li>
 * </ol>
 */
@Slf4j
@Service
public class OperatorSseRegistry implements OperatorSseRegistryService {

    /**
     * operatorId → active SseEmitter.
     * A new entry is created when the operator opens the SSE endpoint.
     * The entry is removed on timeout, completion, or error.
     */
    private final Map<String, SseEmitter> registry = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE connection for an operator.
     *
     * <p>If the operator reconnects (e.g., app backgrounded and resumed),
     * the old emitter is replaced. The old emitter's completion/error callbacks
     * will fire harmlessly since we no longer hold a reference to it.
     *
     * @param operatorId The user-ID of the connecting operator.
     * @param timeout    Emitter timeout in milliseconds (0 = no timeout).
     * @return The newly created {@link SseEmitter}.
     */
    public SseEmitter register(String operatorId, long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);

        // Clean up on any terminal event so the map doesn't grow unbounded
        emitter.onCompletion(() -> {
            registry.remove(operatorId);
            log.debug("[SSE] Emitter completed for operator {}", operatorId);
        });
        emitter.onTimeout(() -> {
            registry.remove(operatorId);
            log.debug("[SSE] Emitter timed out for operator {}", operatorId);
        });
        emitter.onError(ex -> {
            registry.remove(operatorId);
            log.warn("[SSE] Emitter error for operator {}: {}", operatorId, ex.getMessage());
        });

        registry.put(operatorId, emitter);
        log.info("[SSE] Operator {} connected. Active connections: {}", operatorId, registry.size());
        return emitter;
    }

    /**
     * Pushes a named event to a specific operator's SSE stream.
     *
     * <p>Silently no-ops if the operator has no active connection (e.g., the app
     * is offline). The task is already persisted in the DB; the operator will
     * see it when they reconnect and call {@code GET /api/v1/tasks/current}.
     *
     * @param operatorId The target operator.
     * @param eventName  The SSE event name (e.g., {@code "TASK_ASSIGNED"}).
     * @param payload    The object to serialize as JSON in the event data field.
     */
    public void sendToOperator(String operatorId, String eventName, Object payload) {
        SseEmitter emitter = registry.get(operatorId);
        if (emitter == null) {
            log.debug("[SSE] No active connection for operator {} — task persisted, will sync on reconnect.",
                    operatorId);
            return;
        }

        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(payload)
            );
            log.info("[SSE] Event '{}' pushed to operator {}", eventName, operatorId);
        } catch (IOException e) {
            log.warn("[SSE] Failed to push to operator {} — removing stale emitter: {}",
                    operatorId, e.getMessage());
            registry.remove(operatorId);
            emitter.completeWithError(e);
        }
    }

    /**
     * Returns whether an operator currently has an active SSE connection.
     * Exposed for health-check/debug endpoints.
     */
    public boolean isConnected(String operatorId) {
        return registry.containsKey(operatorId);
    }

    /** Returns the total number of live operator connections. */
    public int connectionCount() {
        return registry.size();
    }
}
