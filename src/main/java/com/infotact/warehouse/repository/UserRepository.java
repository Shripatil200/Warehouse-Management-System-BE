package com.infotact.warehouse.repository;

import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.OperatorStatus;
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

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"warehouse"})
    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

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

    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId " +
            "AND u.status = :status")
    List<User> findByWarehouseAndStatus(
            @Param("warehouseId") String warehouseId,
            @Param("status") UserStatus status);

    @Query("SELECT u FROM User u JOIN FETCH u.warehouse " +
            "WHERE u.warehouse.id = :warehouseId AND u.role = :role " +
            "AND u.status <> :excludedStatus")
    List<User> findByWarehouseAndRole(
            @Param("warehouseId") String warehouseId,
            @Param("role") Role role,
            @Param("excludedStatus") UserStatus excludedStatus);

    boolean existsByEmail(@Email @NotBlank String email);

    boolean existsByContactNumber(
            @NotBlank @Size(min = 10, max = 15) @Pattern(regexp = "^\\d+$") String contactNumber);

    @Query("SELECT u.status, COUNT(u) FROM User u WHERE u.warehouse.id = :warehouseId GROUP BY u.status")
    List<Object[]> countUsersByStatus(@Param("warehouseId") String warehouseId);

    @Query("SELECT COUNT(DISTINCT u.warehouse.id) FROM User u WHERE u.email = :email AND u.warehouse IS NOT NULL")
    long countAssignedWarehouseForUser(@Param("email") String email);

    /**
     * Finds the first AVAILABLE operator in the given warehouse.
     *
     * <p>Spring Data JPA derives the query automatically from the method name:
     * {@code WHERE warehouse_id = ? AND operator_status = ?}.
     *
     * <p>The "first" guarantees we always get at most one result even if several
     * operators are AVAILABLE — the engine will assign more tasks as completions
     * trickle in.  No explicit LIMIT needed; Spring Data handles it.
     */
    Optional<User> findFirstByWarehouseIdAndOperatorStatus(
            String warehouseId,
            OperatorStatus operatorStatus
    );

    /**
     * Returns all operators with AVAILABLE status in a warehouse.
     * Used by the manager dashboard to show operator capacity.
     */
    List<User> findByWarehouseIdAndOperatorStatus(
            String warehouseId,
            OperatorStatus operatorStatus
    );
}
