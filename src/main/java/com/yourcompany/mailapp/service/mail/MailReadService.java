package com.yourcompany.mailapp.service.mail;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class MailReadService {

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private int imapPort;

    public List<Map<String, String>> readInbox(String email, String password)
            throws Exception {

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore();
        store.connect(imapHost, imapPort, email, password);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        List<Map<String, String>> mails = new ArrayList<>();

        for (Message msg : inbox.getMessages()) {
            mails.add(Map.of(
                    "from", msg.getFrom()[0].toString(),
                    "subject", msg.getSubject()));
        }

        inbox.close(false);
        store.close();
        return mails;
    }
}
