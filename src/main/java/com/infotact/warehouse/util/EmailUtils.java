package com.infotact.warehouse.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
 * <p>
 * This service handles system-to-user notifications including facility onboarding,
 * password recovery, and security alerts. All methods are executed in a separate
 * thread pool to prevent blocking main business transactions.
 * </p>
 */
@Service
public class EmailUtils {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Sends a rich-text HTML welcome email to a new Warehouse Administrator.
     * <p>
     * <b>Operational Context:</b> Triggered at the end of the facility creation
     * process. It communicates the deterministic password rule to the admin,
     * bridging the gap between database creation and user access.
     * </p>
     * @param to The administrator's email.
     * @param adminName The name of the person being onboarded.
     * @param warehouseName The name of the newly initialized facility.
     */
    @Async
    public void sendWarehouseWelcomeEmail(String to, String adminName, String warehouseName) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("🚀 Welcome to InfoTact - Your Warehouse is Ready!");

        // Professional HTML template with brand colors and clear CTA (Call to Action)
        String htmlMsg = "..."; // [Email HTML Content]

        helper.setText(htmlMsg, true);
        emailSender.send(message);
    }

    /**
     * General purpose utility for sending plain-text messages with CC support.
     */
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

    /**
     * Sends a secure, time-bound password reset link.
     * <p>
     * <b>Security Note:</b> The URL includes a unique UUID token. The template
     * explicitly warns the user of the 15-minute expiration period to minimize
     * the window for unauthorized account access.
     * </p>
     */
    @Async
    public void forgetPasswordMail(String to, String subject, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        // Security-focused template with reset link
        String htmlMsg = "..."; // [Reset HTML Content]

        helper.setText(htmlMsg, true);
        emailSender.send(message);
    }

    /**
     * Security notification sent after a successful password change.
     * Acts as an audit trail to alert users in case of unauthorized profile updates.
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

    /**
     * Converts a List of CC recipients into a standard String array for the MailSender.
     */
    private String[] getCcArray(List<String> ccList) {
        String[] cc = new String[ccList.size()];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = ccList.get(i);
        }
        return cc;
    }
}