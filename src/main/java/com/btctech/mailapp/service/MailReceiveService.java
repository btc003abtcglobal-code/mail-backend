package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.MailResponse;
import com.btctech.mailapp.exception.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
public class MailReceiveService {

    @Value("${mail.imap.host:localhost}")
    private String imapHost;

    @Value("${mail.imap.port:143}")
    private int imapPort;

    /**
     * Read inbox using per-request credentials
     */
    public List<MailResponse> readInbox(String email, String password) {
        log.info("Reading inbox for: {}", email);

        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.host", imapHost);
        props.put("mail.imap.port", imapPort);
        // props.put("mail.imap.starttls.enable", "true"); // Optional

        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore("imap");
            store.connect(imapHost, email, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            List<MailResponse> mailList = new ArrayList<>();

            // Fetch last 20 messages for performance, or all if less
            int start = Math.max(0, messages.length - 20);
            for (int i = messages.length - 1; i >= start; i--) {
                Message msg = messages[i];
                MailResponse response = new MailResponse();
                response.setUid(String.valueOf(msg.getMessageNumber())); // Simple UID
                // response.setMessageId(...); // If needed
                response.setFrom(InternetAddress.toString(msg.getFrom()));
                response.setTo(InternetAddress.toString(msg.getRecipients(Message.RecipientType.TO)));
                response.setSubject(msg.getSubject());
                response.setSentDate(msg.getSentDate().toString()); // Format as string for simplicity

                // Body preview (simplified)
                Object content = msg.getContent();
                if (content instanceof String) {
                    response.setBodyPreview((String) content); // Or substring
                } else if (content instanceof Multipart) {
                    // Extract text from multipart
                    response.setBodyPreview("[Multipart Content]");
                }

                mailList.add(response);
            }

            inbox.close(false);
            store.close();

            return mailList;

        } catch (Exception e) {
            log.error("Failed to read inbox", e);
            throw new MailException("Failed to read inbox: " + e.getMessage());
        }
    }
}
