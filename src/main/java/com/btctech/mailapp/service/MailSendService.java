package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
public class MailSendService {

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.port}")
    private int smtpPort;

    /**
     * Send email using per-request credentials
     */
    public void sendMail(String fromEmail, String password, SendMailRequest request) {
        log.info("Sending email from: {} to: {}", fromEmail, request.getTo());

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Or false depending on local setup? usually false for internal
                                                        // postfix
        // If local postfix doesn't support TLS, use:
        // props.put("mail.smtp.starttls.enable", "false");
        // But assumed standard setup. Let's stick to simple "true" or defaults.
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", "*"); // For self-signed certs

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(request.getCc()));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(request.getBcc()));
            }

            message.setSubject(request.getSubject());

            if (Boolean.TRUE.equals(request.getIsHtml())) {
                message.setContent(request.getBody(), "text/html; charset=utf-8");
            } else {
                message.setText(request.getBody());
            }

            Transport.send(message);
            log.info("Email sent successfully");

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new MailException("Failed to send email: " + e.getMessage());
        }
    }
}
