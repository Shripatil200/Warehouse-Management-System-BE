package com.infotact.warehouse.entity.base;

import com.infotact.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Base class for all entities that are scoped to a specific warehouse.
 *
 * <p>Every subclass automatically gets a {@code warehouse_id} foreign key column
 * and participates in the Hibernate {@code warehouseFilter}, which is activated
 * per-request by {@link com.infotact.warehouse.config.WarehouseContextFilter}.
 * This means any JPQL/Criteria query against a subclass entity will silently
 * append {@code WHERE warehouse_id = :warehouseId}, ensuring data from one
 * warehouse is never visible to users of another.</p>
 *
 * <p>The {@code @FilterDef} for {@code warehouseFilter} is declared once on
 * {@link com.infotact.warehouse.entity.InventoryItem} (the first entity in
 * the Hibernate session that Hibernate processes). The {@code @Filter} here
 * applies that definition to every subclass table.</p>
 */
@Getter
@Setter
@MappedSuperclass
@Filter(
        name = "warehouseFilter",
        condition = "warehouse_id = :warehouseId"
)
public abstract class WarehouseScopedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
}
