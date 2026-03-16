package com.infotact.warehouse.entity;

import com.infotact.warehouse.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouses")
@EqualsAndHashCode(callSuper = true)
public class Warehouse extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String location;

    private boolean active = true;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL)
    private List<Zone> zones;
}
