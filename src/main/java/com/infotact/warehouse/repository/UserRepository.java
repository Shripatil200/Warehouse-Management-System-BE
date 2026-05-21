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
 */
@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"warehouse"})
    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    /**
     * Retrieves non-deleted staff for a specific facility.
     */
    @Query(value = "SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status <> :excludedStatus",
            countQuery = "SELECT count(u) FROM User u " +
                    "WHERE u.warehouse.id = :warehouseId " +
                    "AND u.status <> :excludedStatus")
    Page<User> findAllByWarehouse(
            @Param("warehouseId") String warehouseId,
            @Param("excludedStatus") UserStatus excludedStatus,
            Pageable pageable);

    /**
     * Retrieves currently operational staff.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status = :status")
    List<User> findByWarehouseAndStatus(
            @Param("warehouseId") String warehouseId,
            @Param("status") UserStatus status);

    /**
     * Filters staff by role within a warehouse.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId AND u.role = :role " +
            "AND u.status <> :excludedStatus")
    List<User> findByWarehouseAndRole(
            @Param("warehouseId") String warehouseId,
            @Param("role") Role role,
            @Param("excludedStatus") UserStatus excludedStatus);

    /**
     * Used by legacy internal callers that don't need pagination.
     */
    Collection<User> findByRole(Role role);

    /**
     * Returns a paginated list of users with a specific role.
     * Used by {@code SupplierServiceImpl.getAllSuppliers()} to list all
     * registered supplier accounts globally.
     */
    Page<User> findByRole(Role role, Pageable pageable);

    boolean existsByEmail(@Email @NotBlank String email);

    boolean existsByContactNumber(
            @NotBlank @Size(min = 10, max = 15) @Pattern(regexp = "^\\d+$") String contactNumber);

    @Query("SELECT COUNT(DISTINCT u.warehouse.id) FROM User u WHERE u.email = :email AND u.warehouse IS NOT NULL")
    long countAssignedWarehouseForUser(@Param("email") String email);

    /**
     * Dashboard aggregation for user statuses.
     */
    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.warehouse.id = :warehouseId GROUP BY u.status")
    List<Object[]> countUsersByStatus(@Param("warehouseId") String warehouseId);
}