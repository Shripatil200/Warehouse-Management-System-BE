package com.infotact.warehouse.config;

/**
 * @deprecated Use {@link WarehouseContext} instead.
 * Kept as a thin delegate so existing call-sites compile without a big-bang rename.
 */
@Deprecated
public class WarehouseContext {

    public static void set(String warehouseId)  { WarehouseContext.set(warehouseId); }
    public static String get()                  { return WarehouseContext.get(); }
    public static void clear()                  { WarehouseContext.clear(); }
}
