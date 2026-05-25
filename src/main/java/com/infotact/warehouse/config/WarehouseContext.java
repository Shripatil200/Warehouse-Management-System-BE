package com.infotact.warehouse.config;

/**
 * Thread-local holder for the warehouse ID of the currently authenticated user.
 * <p>
 * Set by {@link WarehouseContextFilter} at the start of each request and cleared
 * in the {@code finally} block to prevent context leaks across thread-pool reuse.
 * </p>
 */
public class WarehouseContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String warehouseId) {
        CURRENT.set(warehouseId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
