package com.infotact.warehouse.repository;

import java.util.List;
import java.util.Optional;

import com.infotact.warehouse.dto.v1.response.UserResponse;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;

import com.infotact.warehouse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // 1. Standard finding method (no @Query needed if field name matches email)
    Optional<User> findByEmail(String email);

    // 2. Fetching Custom Wrapper (You must provide the JPQL constructor expression)
    @Query("SELECT new com.infotact.warehouse.dto.v1.response.UserResponse(u.id, u.name, u.email, u.contactNumber, u.status, u.role) FROM User u")
    List<UserResponse> getAllUser();

    // 3. Fetching only emails of Admins
    @Query("SELECT u.email FROM User u WHERE u.role = 'ADMIN'")
    List<String> getAllAdmin();

    // 4. Update status with explicit JPQL
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    Integer updateStatus(@Param("status") String status, @Param("id") String id);

    // 5. Update password with explicit JPQL
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    Integer updatePassword(@Param("password") String password, @Param("id") String id);

}