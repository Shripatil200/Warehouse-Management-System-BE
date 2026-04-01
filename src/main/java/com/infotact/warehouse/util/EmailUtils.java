package com.infotact.warehouse.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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


    public void forgetPasswordMail(String to, String subject, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String htmlMsg =
                "<p><b>Hello,</b><br>" +
                        "We received a request to reset your password.<br><br>" +
                        "<a href='" + frontendUrl + "/reset-password?token=" + token + "'>" +
                        "Reset Password</a><br><br>" +
                        "This link will expire in 15 minutes.<br>" +
                        "If you didn't request this, ignore this email.</p>";
        message.setContent(htmlMsg, "text/html");
        emailSender.send(message);
    }

    public void passwordUpdatedEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }


}
