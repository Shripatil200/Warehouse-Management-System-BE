package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Identity and Access Management (IAM).
 * <p>
 * This repository handles authentication lookups and facility-based staff management.
 * By extending {@link JpaSpecificationExecutor}, it supports dynamic, complex filtering
 * required for advanced warehouse staff searches.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    /**
     * Primary lookup for the AuthenticationProvider during login.
     */
    Optional<User> findByEmail(String email);

    /**
     * Unique contact lookup for registration and validation.
     */
    Optional<User> findByContactNumber(String contactNumber);

    /**
     * Retrieves a paginated list of non-deleted staff members for a specific facility.
     * <p>
     * <b>Optimization:</b> Uses 'JOIN FETCH' for the result list to prevent N+1 issues.
     * </p>
     */
    @Query(value = "SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED",
            countQuery = "SELECT count(u) FROM User u " +
                    "WHERE u.warehouse.id = :warehouseId " +
                    "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    Page<User> findAllByWarehouse(@Param("warehouseId") String warehouseId, Pageable pageable);

    /**
     * Retrieves currently operational staff for a specific facility.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status = com.infotact.warehouse.entity.enums.UserStatus.ACTIVE")
    List<User> findActiveByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Filters staff by specialized responsibility within a facility.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId AND u.role = :role " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    List<User> findByWarehouseAndRole(@Param("warehouseId") String warehouseId, @Param("role") Role role);

    /**
     * Global role lookup. Primarily used for system-wide administrative tasks.
     */
    Collection<User> findByRole(Role role);

    /**
     * Checks if an email is already registered in the system.
     */
    boolean existsByEmail(@Email @NotBlank String email);

    /**
     * Checks if a contact number is already registered in the system.
     */
    boolean existsByContactNumber(@NotBlank @Size(min = 10, max = 15) @Pattern(regexp = "^\\d+$") String contactNumber);

    /**
     * Counts unique warehouses assigned to a specific email for multi-tenant validation.
     */
    @Query("SELECT COUNT(DISTINCT u.warehouse.id) FROM User u WHERE u.email = :email AND u.warehouse IS NOT NULL")
    long countAssignedWarehouseForUser(@Param("email") String email);
}