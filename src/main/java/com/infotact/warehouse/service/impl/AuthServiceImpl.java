package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.*;
import com.infotact.warehouse.config.JWT.JwtFilter;
import com.infotact.warehouse.config.JWT.JwtUtil;
import com.infotact.warehouse.config.JWT.UsersDetailsService;
import com.infotact.warehouse.dto.v1.request.UserRequest;
import com.infotact.warehouse.dto.v1.response.UserResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.exception.AccessDeniedException;
import com.infotact.warehouse.exception.BadRequestException;
import com.infotact.warehouse.exception.ResourceNotFoundException;
import com.infotact.warehouse.exception.UnauthorizedException;
import com.infotact.warehouse.repository.ResetPasswordRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.service.AuthService;
import com.infotact.warehouse.util.EmailUtils;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link AuthService}.
 * Manages user authentication lifecycle and administrative permissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final ResetPasswordRepository resetPasswordRepository;
    private final JwtUtil jwtUtil;
    private final EmailUtils emailUtils;
    private final PasswordEncoder passwordEncoder;

    // HELPER: Replaces the need for injecting JwtFilter
    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    @Override
    public String login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (auth.isAuthenticated()) {
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new UnauthorizedException("User record not found."));

                if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    throw new AccessDeniedException("Account is pending for admin approval.");
                }

                return "{\"token\":\"" + jwtUtil.generateToken(user.getEmail(), user.getRole().name()) + "\"}";
            }
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        }
        throw new BadRequestException("Authentication failed");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUser() {
        if (!hasRole("ADMIN")) {
            throw new UnauthorizedException("Admin rights required.");
        }
        return userRepository.getAllUser();
    }

    @Override
    @Transactional
    public void updateStatus(String userId, Boolean status) {
        if (!hasRole("ADMIN")) {
            throw new UnauthorizedException("Admin rights required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String status_ = status ? "ACTIVE": "INACTIVE";
        user.setStatus(status_);
        userRepository.save(user); // Standard save is safer than custom update queries
    }

    @Override
    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        String currentEmail = getAuthenticatedUserEmail();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect old password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password updated successfully.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String createUser(UserRequest request) {
        // Authorization check using helper
        if (!hasRole("ADMIN") && !hasRole("MANAGER")) {
            throw new UnauthorizedException("Access denied.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered.");
        }

        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setContactNumber(request.getContactNumber());
        newUser.setRole(com.infotact.warehouse.entity.Role.EMPLOYEE); // Or request.getRole()
        newUser.setStatus("INACTIVE");

        // Better fallback for substring to avoid StringIndexOutOfBoundsException
        String phone = request.getContactNumber();
        String lastFour = (phone.length() >= 4) ? phone.substring(phone.length() - 4) : "1234";
        String tempPassword = "Welcome@" + lastFour;

        newUser.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(newUser);

        // ... Email logic ...
        // 5. Notify the employee via email
        try {
            emailUtils.passwordUpdatedEmail(
                    newUser.getEmail(),
                    "Warehouse Account Created",
                    "Your account has been created by your manager.\n\n" +
                            "Username: " + newUser.getEmail() + "\n" +
                            "Temporary Password: " + tempPassword + "\n\n" +
                            "Please log in and change your password immediately."
            );
        } catch (Exception e) {
            log.warn("Account created, but failed to send welcome email to {}", newUser.getEmail());
        }
        return "User created successfully. Temporary credentials sent to " + newUser.getEmail();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String forgotPassword(String email) {
        log.info("Forgot password link requested for email: {}", email);

        // Process only if user exists to prevent email enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            ResetPasswordToken resetToken = new ResetPasswordToken(token, user.getEmail());
            resetPasswordRepository.save(resetToken);

            try {
                emailUtils.forgetPasswordMail(user.getEmail(), "Reset Password", token);
            } catch (MessagingException e) {
                log.error("Failed to send reset email to {}: {}", user.getEmail(), e.getMessage());
                // Use a custom exception that you have defined in your handler
                throw new BadRequestException("We encountered an error sending your reset link. Please try again.");
            }
        });

        return "Reset link sent to your email.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password using provided token.");

        ResetPasswordToken tokenObj = resetPasswordRepository.findById(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        User user = userRepository.findByEmail(tokenObj.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // One-time use: Delete token after successful reset
        resetPasswordRepository.delete(tokenObj);

        emailUtils.passwordUpdatedEmail(user.getEmail(), "Password Updated", "Your password has been updated successfully.");

        return "Password changed successfully";
    }

}