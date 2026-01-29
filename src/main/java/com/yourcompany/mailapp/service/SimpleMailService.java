package com.yourcompany.mailapp.service;

import com.yourcompany.mailapp.dto.MailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Multipart;
import jakarta.mail.BodyPart;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class SimpleMailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendMail(String from, String to, String subject, String body) {
        log.info("Sending mail from: {} to: {}", from, to);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from + "@localhost");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Mail sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send mail to {}", to, e);
            throw e; // Rethrow to let controller handle it
        }
    }

    public void sendMailWithAttachment(String from, String to, String subject, String body,
            org.springframework.web.multipart.MultipartFile file) throws Exception {
        log.info("Sending mail with attachment from: {} to: {}", from, to);

        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
        org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                message, true);

        helper.setFrom(from + "@localhost");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        if (file != null && !file.isEmpty()) {
            helper.addAttachment(file.getOriginalFilename(), file);
        }

        mailSender.send(message);
        log.info("Mail with attachment sent successfully to {}", to);
    }

    public List<MailResponse> fetchEmails(String username, String password) throws Exception {

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", "localhost");
        props.put("mail.imap.port", "143");
        props.put("mail.imap.ssl.enable", "false");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imap");

        store.connect("localhost", username, password);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        List<MailResponse> mails = new ArrayList<>();

        for (Message msg : inbox.getMessages()) {

            String from = (msg.getFrom() != null && msg.getFrom().length > 0) ? msg.getFrom()[0].toString() : "Unknown";
            String subject = msg.getSubject();
            String body = getTextFromMessage(msg); // ✅ FIX

            mails.add(new MailResponse(from, subject, body));
        }

        inbox.close(false);
        store.close();

        return mails;
    }

    private String getTextFromMessage(Message message) throws Exception {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart mimeMultipart = (Multipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        } else if (message.isMimeType("text/html")) {
            String html = (String) message.getContent();
            result = org.jsoup.Jsoup.parse(html).text();
        }
        return result;
    }

    private String getTextFromMimeMultipart(Multipart mimeMultipart) throws Exception {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof Multipart) {
                result = result + getTextFromMimeMultipart((Multipart) bodyPart.getContent());
            }
        }
        return result;
    }

}