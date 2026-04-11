package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
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
 * It utilizes eager fetching for the {@link com.infotact.warehouse.entity.Warehouse}
 * relationship to optimize performance during authorization checks.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Primary lookup for the AuthenticationProvider.
     */
    Optional<User> findByEmail(String email);

    /**
     * Unique contact lookup for registration and validation.
     */
    Optional<User> findByContactNumber(String contactNumber);

    /**
     * Retrieves all non-deleted staff members for a specific facility.
     * <p>
     * Optimization: Uses 'JOIN FETCH' to retrieve user and warehouse data in a
     * single query, preventing N+1 performance issues during list rendering.
     * </p>
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    List<User> findAllByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Retrieves currently operational staff for a specific facility.
     * <p>
     * Usage: Used to populate assignable staff lists for tasks like
     * Picking or Cycle Counting.
     * </p>
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status = com.infotact.warehouse.entity.enums.UserStatus.ACTIVE")
    List<User> findActiveByWarehouse(@Param("warehouseId") String warehouseId);

    /**
     * Filters staff by specialized responsibility within a facility.
     * @param role The authorization level (e.g., ADMIN, MANAGER).
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId AND u.role = :role " +
            "AND u.status <> com.infotact.warehouse.entity.enums.UserStatus.DELETED")
    List<User> findByWarehouseAndRole(@Param("warehouseId") String warehouseId, @Param("role") Role role);

    /**
     * Global role lookup. Primarily used for system-wide administrative tasks.
     */
    Collection<User> findByRole(Role role);

    boolean existsByEmail(@Email(message = "Invalid email format") @NotBlank(message = "Admin email is required") String adminEmail);

    boolean existsByContactNumber(@NotBlank(message = "Contact number is required") @Size(min = 10, max = 15) @Pattern(regexp = "^\\d+$", message = "Contact number must contain only digits") String adminContact);

    /**
     * Counts the number of unique warehouses assigned to a specific user email.
     * Use DISTINCT to ensure we don't over-count if the user has multiple roles/records.
     */
    @Query("SELECT COUNT(DISTINCT u.warehouse.id) FROM User u WHERE u.email = :email AND u.warehouse IS NOT NULL")
    long countAssignedWarehouseForUser(@Param("email") String email);
}