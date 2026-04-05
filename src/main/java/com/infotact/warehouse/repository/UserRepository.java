package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    // Optimized: Fetch all users belonging to a specific warehouse
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    List<User> findAllByWarehouse(@Param("warehouseId") String warehouseId);

    // Optimized: Fetch only ACTIVE users for a specific warehouse
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status = com.infotact.warehouse.entity.enums.UserStatus.ACTIVE")
    List<User> findActiveByWarehouse(@Param("warehouseId") String warehouseId);

    // Optimized: Fetch by role within a specific warehouse
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId AND u.role = :role " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    List<User> findByWarehouseAndRole(@Param("warehouseId") String warehouseId, @Param("role") Role role);

    Collection<User> findByRole(Role role);
}