package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Zone;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, String> {
    boolean existsByNameAndWarehouseId(String name, String warehouseId);
}
