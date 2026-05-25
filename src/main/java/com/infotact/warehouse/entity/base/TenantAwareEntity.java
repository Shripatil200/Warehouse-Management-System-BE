package com.infotact.warehouse.entity.base;

/**
 * @deprecated Use {@link WarehouseScopedEntity} instead.
 * Retained as a compile-time alias so existing entity subclasses continue
 * to work without a mass rename. Will be removed in a future cleanup pass.
 */
@Deprecated
public abstract class TenantAwareEntity extends WarehouseScopedEntity {
    // intentionally empty — all functionality is in WarehouseScopedEntity
}
