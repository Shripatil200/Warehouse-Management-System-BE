package com.infotact.warehouse.service;

import com.infotact.warehouse.common_wrappers.ChangePasswordRequest;
import com.infotact.warehouse.common_wrappers.LoginRequest;
import com.infotact.warehouse.common_wrappers.ResetPasswordRequest;
import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Service interface for managing authentication and user administration.
 */
public interface AuthService {

    /**
     * Authenticates a user and returns a JWT token.
     * * @param request the login credentials (email and password)
     * @return a JSON string containing the generated JWT token
     */
    String login(LoginRequest request);




    String changePassword(ChangePasswordRequest request);

    String forgotPassword(String email);

    String resetPassword(ResetPasswordRequest request);


}