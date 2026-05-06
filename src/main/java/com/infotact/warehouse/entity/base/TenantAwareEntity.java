package com.infotact.warehouse.entity.base;

import com.infotact.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;


@Getter
@Setter
@MappedSuperclass



@Filter(
        name = "warehouseFilter",
        condition = "warehouse_id = :warehouseId"
)
public abstract class TenantAwareEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
}