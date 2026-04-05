package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.*;
import com.infotact.warehouse.config.JWT.JwtUtil;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link AuthService} handling core security logic.
 * Uses Spring Security's AuthenticationManager and JWT for stateless sessions.
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

    /**
     * Utility to extract email from the Security Context of the current request.
     */
    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    /** {@inheritDoc} */
    @Override
    public String login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (auth.isAuthenticated()) {
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new UnauthorizedException("User record not found."));

                // Business Logic: Only ACTIVE accounts can log in
                if (user.getStatus() != UserStatus.ACTIVE) {
                    throw new AccessDeniedException("Account is " + user.getStatus() + ". Please contact Admin.");
                }

                return "{\"token\":\"" + jwtUtil.generateToken(user.getEmail(), user.getRole().name()) + "\"}";
            }
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        }
        throw new BadRequestException("Authentication failed");
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String forgotPassword(String email) {
        log.info("Forgot password link requested for email: {}", email);

        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            ResetPasswordToken resetToken = new ResetPasswordToken(token, user.getEmail());
            resetPasswordRepository.save(resetToken);

            try {
                emailUtils.forgetPasswordMail(user.getEmail(), "Reset Password", token);
            } catch (MessagingException e) {
                log.error("Failed to send reset email to {}: {}", user.getEmail(), e.getMessage());
                throw new BadRequestException("Error sending reset link. Please try again.");
            }
        });

        return "Reset link sent to your email.";
    }

    /** {@inheritDoc} */
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

        // Token cleanup after successful use
        resetPasswordRepository.delete(tokenObj);

        try {
            emailUtils.passwordUpdatedEmail(user.getEmail(), "Password Updated", "Your password has been updated successfully.");
        } catch (Exception e) {
            log.warn("Password reset successful, but notification email failed for {}", user.getEmail());
        }

        return "Password changed successfully";
    }
}