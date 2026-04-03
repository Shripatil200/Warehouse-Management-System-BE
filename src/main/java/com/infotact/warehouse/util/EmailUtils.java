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

@Service
public class EmailUtils {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Value("${app.frontend.url}")
    private String frontendUrl;

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
        String[] cc = new String[ccList.size()];

        for (int i = 0; i < cc.length; i++) {
            cc[i] = ccList.get(i);
        }

        return cc;
    }

    @Async
    public void forgetPasswordMail(String to, String subject, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        // Use UTF-8 encoding for modern mail clients
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        String htmlMsg =
                "<div style='font-family: Arial, sans-serif;'>" +
                        "<h3>Hello,</h3>" +
                        "<p>We received a request to reset your password.</p>" +
                        "<a href='" + frontendUrl + "/reset-password?token=" + token + "' " +
                        "style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>" +
                        "Reset Password</a>" +
                        "<p><br>This link will expire in 15 minutes.<br>" +
                        "If you didn't request this, ignore this email.</p>" +
                        "</div>";

        // ✅ Use the helper to set HTML content properly
        helper.setText(htmlMsg, true);

        emailSender.send(message);
    }

    @Async
    public void passwordUpdatedEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }



}
