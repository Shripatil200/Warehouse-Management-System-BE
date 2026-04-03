package com.infotact.warehouse.service;

import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.request.UserUpdate;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.enums.Role;
import com.infotact.warehouse.entity.enums.UserStatus;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {

    String createUser(@Valid UserRequest request);

    /**
     * Retrieves a list of all users in the system.
     * Access is restricted to users with ADMIN roles.
     * * @return a list of UserResponse objects
     */
    List<UserResponse> getAllUser();

    /**
     * Updates the status (enable/disable) of a specific user.
     * * @param user ID and the new status
     */
    void updateStatus(String id, UserStatus status);

    String updateUserDetails(String id, UserUpdate request);

    UserResponse getUserById(String id);


    void deleteUser(String id);

    List<UserResponse> getUsersByRole(Role role);


    List<UserResponse> getAllActiveUsers();
}
