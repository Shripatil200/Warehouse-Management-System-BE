package com.infotact.warehouse.service.impl;

import com.infotact.warehouse.common_wrappers.*;
import com.infotact.warehouse.config.JWT.JwtUtil;
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
 * and persistent user records. It enforces account-status guardrails (e.g., blocking
 * INACTIVE users) and manages the secure lifecycle of credential recovery.
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
     * @return The email associated with the current SecurityContext.
     */
    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    /** * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Uses Spring Security's {@link AuthenticationManager} for credential verification.</li>
     * <li>Strictly prevents logins for accounts that are not in {@link UserStatus#ACTIVE} state.</li>
     * <li><b>Context Injection:</b> Returns a structured object containing the JWT and
     * facility-specific metadata (WarehouseID) to assist frontend routing.</li>
     * </ul>
     */
    @Override
    public AuthResponse login(LoginRequest request) { // Method signature changed to AuthResponse
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (auth.isAuthenticated()) {
                User user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new UnauthorizedException("User record not found."));

                if (user.getStatus() != UserStatus.ACTIVE) {
                    log.warn("Login blocked: Account {} is currently {}", user.getEmail(), user.getStatus());
                    throw new AccessDeniedException("Account is " + user.getStatus() + ". Please contact Admin.");
                }

                log.info("Login successful for user: {}", user.getEmail());

                // Generate the token
                String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

                // Return the structured object
                return AuthResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .warehouseId(user.getWarehouse() != null ? user.getWarehouse().getId() : null)
                        .build();
            }
        } catch (AuthenticationException ex) {
            throw new BadRequestException("Invalid email or password");
        }
        throw new BadRequestException("Authentication failed");
    }

    /** * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Resolves the user via the authenticated SecurityContext.</li>
     * <li>Performs manual {@link PasswordEncoder#matches} check before applying updates.</li>
     * </ul>
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

    /** * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Generates a random UUID as a reset token.</li>
     * <li>Persists the token-email mapping via {@link ResetPasswordRepository}.</li>
     * <li>Fails silently for non-existent emails to prevent account enumeration.</li>
     * </ul>
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

    /** * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * <ul>
     * <li>Invalidates the token immediately after successful use to prevent Replay Attacks.</li>
     * <li>Sends a confirmation email notifying the user of the successful security update.</li>
     * </ul>
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

        // Token cleanup after successful use
        resetPasswordRepository.delete(tokenObj);

        try {
            emailUtils.passwordUpdatedEmail(user.getEmail(), "Password Updated", "Your password has been updated successfully.");
        } catch (Exception e) {
            log.warn("Password reset successful, but notification email failed for {}", user.getEmail());
        }

        return "Password changed successfully";
    }

    /**
     * Sends a 6-digit OTP to email.
     * Clears any existing OTP for this email first.
     */
    @Override
    public void sendEmailOtp(String email) {
        // 1. Clear previous OTP if user clicked 'Resend'
        otpRepo.findById(email).ifPresent(otpRepo::delete);

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        otpRepo.save(new OtpToken(email, otp));

        try {
            emailUtils.sendOtpMail(email, "Email Verification Code", otp);
            log.info("Fresh OTP sent to email: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email OTP to {}: {}", email, e.getMessage());
            throw new BadRequestException("Failed to send email. Please try again.");
        }
    }

    /**
     * Verifies Email OTP.
     * Deletes the OTP immediately so it cannot be reused.
     */
    @Override
    public String processEmailVerification(String email, String otp) {
        OtpToken cached = otpRepo.findById(email)
                .orElseThrow(() -> new BadRequestException("OTP expired or not found. Please resend."));

        if (!cached.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP code.");
        }

        // 2. Consume OTP immediately on success
        otpRepo.delete(cached);

        String vToken = "eml-" + UUID.randomUUID();
        proofRepo.save(new VerifiedProof(vToken, email));
        return vToken;
    }

    /**
     * Sends a 6-digit OTP to contact via Fast2SMS.
     * Clears any existing OTP for this contact first.
     */
    @Override
    public void sendContactOtp(String contact) {
        // 1. Clear previous OTP to ensure only the latest SMS is valid
        otpRepo.findById(contact).ifPresent(otpRepo::delete);

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        otpRepo.save(new OtpToken(contact, otp));

        try {
            smsUtils.sendOtpSms(contact, otp);
            log.info("Fresh Contact OTP sent to: {}", contact);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", contact, e.getMessage());
            throw new BadRequestException("Failed to send SMS. Please check the number or balance.");
        }
    }

    /**
     * Verifies Contact OTP.
     * Deletes the OTP immediately so it cannot be reused.
     */
    @Override
    public String processContactVerification(String contact, String otp) {
        OtpToken cached = otpRepo.findById(contact)
                .orElseThrow(() -> new BadRequestException("OTP expired or not found. Please resend."));

        if (!cached.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP code.");
        }

        // 2. Consume OTP immediately on success
        otpRepo.delete(cached);

        String vToken = "cnt-" + UUID.randomUUID();
        proofRepo.save(new VerifiedProof(vToken, contact));
        return vToken;
    }
}