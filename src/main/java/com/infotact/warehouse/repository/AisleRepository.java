package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.Aisle;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AisleRepository extends JpaRepository<Aisle, String> {

    boolean existsByCodeAndZoneId(String code, String zoneId);
}
