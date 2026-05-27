package com.infotact.warehouse.entity.base;

import com.infotact.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all entities that physically belong to the warehouse.
 *
 * <p>Every subclass carries a {@code warehouse_id} foreign key that links it
 * to the single warehouse record. Queries narrow to the correct warehouse
 * by joining through this column.</p>
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class WarehouseScopedEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
}
