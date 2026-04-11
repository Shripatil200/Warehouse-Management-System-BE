package com.infotact.warehouse.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Utility service for asynchronous outbound email communication.
 */
@Slf4j
@Service
public class EmailUtils {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Dispatches an attractive, security-first onboarding email.
     * Removed default password rules to encourage secure, user-defined credentials.
     */
    @Async
    public void sendWarehouseWelcomeEmail(String to, String adminName, String warehouseName) throws MessagingException {
        log.info("Sending attractive onboarding email to: {}", to);
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String loginUrl = frontendUrl + "/login";

        String htmlContent =
                "<div style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e1e4e8; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>" +
                        // Header Banner
                        "<div style='background: linear-gradient(135deg, #007bff 0%, #0056b3 100%); padding: 30px; text-align: center; color: white;'>" +
                        "<h1 style='margin: 0; font-size: 24px; letter-spacing: 1px;'>Welcome to InfoTact WMS</h1>" +
                        "</div>" +

                        // Body Content
                        "<div style='padding: 40px; background-color: white; line-height: 1.6;'>" +
                        "<h2 style='color: #333;'>Hello, " + adminName + "!</h2>" +
                        "<p style='color: #555; font-size: 16px;'>Great news! Your facility, <span style='color: #007bff; font-weight: bold;'>" + warehouseName + "</span>, is now fully initialized and ready for operations.</p>" +

                        // Account Details Card
                        "<div style='background-color: #f4f7f9; border-radius: 8px; padding: 25px; margin: 25px 0; border: 1px dashed #007bff;'>" +
                        "<h3 style='margin-top: 0; color: #0056b3; font-size: 18px;'>Account Details</h3>" +
                        "<p style='margin: 10px 0;'><strong>Authorized Email:</strong> <br><span style='color: #333; font-family: monospace;'>" + to + "</span></p>" +
                        "<p style='margin: 10px 0; color: #28a745;'><strong>Access Status:</strong> <br>Active & Verified</p>" +
                        "</div>" +

                        // Instructions Section
                        "<div style='margin-bottom: 30px;'>" +
                        "<p style='color: #666;'>To ensure maximum security for your facility data, we do not assign default passwords. Please follow these steps to access your dashboard:</p>" +
                        "<ul style='color: #666; padding-left: 20px;'>" +
                        "<li>Visit the login portal using the link below.</li>" +
                        "<li>Click on <strong>'Forgot Password'</strong> or <strong>'First Time Login'</strong>.</li>" +
                        "<li>Enter your registered email to receive a secure password setup link.</li>" +
                        "</ul>" +
                        "</div>" +

                        // CTA Button
                        "<div style='text-align: center; margin-top: 40px;'>" +
                        "<a href='" + loginUrl + "' style='background-color: #28a745; color: white; padding: 15px 35px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px rgba(40, 167, 69, 0.2);'>Access Your Dashboard</a>" +
                        "</div>" +
                        "</div>" +

                        // Footer
                        "<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #eee;'>" +
                        "<p style='font-size: 12px; color: #999; margin: 0;'>This is an automated system notification regarding your facility setup.</p>" +
                        "<p style='font-size: 12px; color: #999; margin: 5px 0 0 0;'>© 2026 InfoTact Supply Chain Solutions</p>" +
                        "</div>" +
                        "</div>";

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Facility Initialized: Welcome to " + warehouseName);
        helper.setText(htmlContent, true);

        emailSender.send(message);
        log.info("Attractive welcome email dispatched successfully to {}", to);
    }

    /**
     * Sends a secure, time-bound password reset link.
     */
    @Async
    public void forgetPasswordMail(String to, String subject, String token) throws MessagingException {
        log.info("Sending password reset email to: {}", to);
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String htmlMsg = "<div style='font-family: Segoe UI, Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;'>" +
                "<div style='text-align: center;'><h2 style='color: #dc3545;'>Password Reset Request</h2></div>" +
                "<p>Hello,</p>" +
                "<p>We received a request to reset the password for your InfoTact WMS account. Click the button below to set a new password:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + resetUrl + "' style='background-color: #007bff; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Reset My Password</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 0.9em;'><b>Note:</b> This link is valid for <b>15 minutes</b> only. If you did not request this, please ignore this email or contact your administrator if you have concerns.</p>" +
                "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                "<p style='font-size: 0.8em; color: #999; text-align: center;'>InfoTact Security Infrastructure</p>" +
                "</div>";

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlMsg, true);

        emailSender.send(message);
        log.info("Password reset link sent to {}", to);
    }

    /**
     * Sends a numeric OTP for email verification.
     */
    @Async
    public void sendOtpMail(String to, String subject, String otp) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String content = "<div style='font-family: Arial, sans-serif; text-align: center; padding: 20px; border: 1px solid #eee;'>" +
                "<h3>Warehouse System Verification</h3>" +
                "<p>Please use the following One-Time Password (OTP) to verify your account:</p>" +
                "<h1 style='color: #2e6cbb; letter-spacing: 5px;'>" + otp + "</h1>" +
                "<p>This code will expire in <b>5 minutes</b>.</p>" +
                "<br><p style='color: #999; font-size: 0.8em;'>If you did not request this, please ignore this email.</p>" +
                "</div>";

        helper.setText(content, true);
        emailSender.send(message);
    }

    /**
     * Security notification sent after a successful password change.
     */
    @Async
    public void passwordUpdatedEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    @Async
    public void sendSimpleMessage(String to, String subject, String text, List<String> list) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        if (list != null && !list.isEmpty())
            message.setCc(getCcArray(list));
        emailSender.send(message);
    }

    private String[] getCcArray(List<String> ccList) {
        return ccList.toArray(new String[0]);
    }
}