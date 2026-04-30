package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.*;
import com.infotact.warehouse.config.JWT.JwtUtil;
import com.infotact.warehouse.config.JWT.UserPrincipal;
import com.infotact.warehouse.dto.v1.response.AuthResponse;
import com.infotact.warehouse.entity.User;
import com.infotact.warehouse.entity.enums.UserStatus;
import com.infotact.warehouse.exception.*;
import com.infotact.warehouse.repository.OtpTokenRepository;
import com.infotact.warehouse.repository.ResetPasswordRepository;
import com.infotact.warehouse.repository.UserRepository;
import com.infotact.warehouse.repository.VerifiedProofRepository;
import com.infotact.warehouse.service.AuthService;
import com.infotact.warehouse.util.EmailUtils;
import com.infotact.warehouse.util.SmsUtils;
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

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link AuthService} handling core security logic.
 * <p>
 * This class orchestrates the interaction between Spring Security, JWT generation,
 * and persistent user records. It enforces account-status guardrails and manages
 * multi-tenant context injection via {@link UserPrincipal}.
 * </p>
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
    private final OtpTokenRepository otpRepo;
    private final VerifiedProofRepository proofRepo;
    private final SmsUtils smsUtils;

    /**
     * Internal utility to resolve the current session's identity.
     */
    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Authenticates credentials via {@link AuthenticationManager}.</li>
     * <li>Wraps the JPA {@link User} into a {@link UserPrincipal} to extract warehouse context.</li>
     * <li>Generates a JWT containing facility-specific claims for stateless multi-tenancy.</li>
     * </ul>
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // 1. Perform standard Spring Security authentication
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (auth.isAuthenticated()) {
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new UnauthorizedException("User record not found."));

                // 2. Status Guardrail
                if (user.getStatus() != UserStatus.ACTIVE) {
                    log.warn("Login blocked: Account {} is currently {}", user.getEmail(), user.getStatus());
                    throw new AccessDeniedException("Account is " + user.getStatus() + ". Please contact Admin.");
                }

                log.info("Login successful for user: {}", user.getEmail());

                // 3. SECURE CONTEXT INJECTION
                // Wrap the user in UserPrincipal so JwtUtil can access the warehouseId
                UserPrincipal principal = new UserPrincipal(user);

                // 4. Generate Token (Now accepts UserPrincipal to embed warehouse claims)
                String token = jwtUtil.generateToken(principal);

                // 5. Build Response
                return AuthResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .warehouseId(principal.getWarehouseId())
                        .build();
            }
        } catch (AuthenticationException ex) {
            log.error("Authentication failed for {}: {}", request.getEmail(), ex.getMessage());
            throw new BadRequestException("Invalid email or password");
        }
        throw new BadRequestException("Authentication failed");
    }

    /**
     * {@inheritDoc}
     */
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
    public Map<String, String> forgotPassword(String email) {
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

        return Map.of("message", "Reset link sent to your email.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        ResetPasswordToken tokenObj = resetPasswordRepository.findById(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        User user = userRepository.findByEmail(tokenObj.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetPasswordRepository.delete(tokenObj);

        try {
            emailUtils.passwordUpdatedEmail(user.getEmail(), "Password Updated", "Your password has been updated successfully.");
        } catch (Exception e) {
            log.warn("Notification email failed for {}", user.getEmail());
        }

        return "Password changed successfully";
    }

    /**
     * Sends a 6-digit OTP to email.
     */
    @Override
    public void sendEmailOtp(String email) {
        otpRepo.findById(email).ifPresent(otpRepo::delete);

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        otpRepo.save(new OtpToken(email, otp));

        try {
            emailUtils.sendOtpMail(email, "Email Verification Code", otp);
            log.info("Fresh OTP sent to email: {}", email);
        } catch (Exception e) {
            throw new BadRequestException("Failed to send email. Please try again.");
        }
    }

    /**
     * Verifies Email OTP and consumes it.
     */
    @Override
    public String processEmailVerification(String email, String otp) {
        OtpToken cached = otpRepo.findById(email)
                .orElseThrow(() -> new BadRequestException("OTP expired or not found."));

        if (!cached.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP code.");
        }

        otpRepo.delete(cached);

        String vToken = "eml-" + UUID.randomUUID();
        proofRepo.save(new VerifiedProof(vToken, email));
        return vToken;
    }

    /**
     * Sends a 6-digit OTP to contact via Fast2SMS.
     */
    @Override
    public void sendContactOtp(String contact) {
        otpRepo.findById(contact).ifPresent(otpRepo::delete);

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        otpRepo.save(new OtpToken(contact, otp));

        try {
            smsUtils.sendOtpSms(contact, otp);
            log.info("Fresh Contact OTP sent to: {}", contact);
        } catch (Exception e) {
            throw new BadRequestException("Failed to send SMS.");
        }
    }

    /**
     * Verifies Contact OTP and consumes it.
     */
    @Override
    public String processContactVerification(String contact, String otp) {
        OtpToken cached = otpRepo.findById(contact)
                .orElseThrow(() -> new BadRequestException("OTP expired or not found."));

        if (!cached.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP code.");
        }

        otpRepo.delete(cached);

        String vToken = "cnt-" + UUID.randomUUID();
        proofRepo.save(new VerifiedProof(vToken, contact));
        return vToken;
    }
}