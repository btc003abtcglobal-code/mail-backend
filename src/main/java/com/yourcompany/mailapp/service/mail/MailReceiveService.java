package com.yourcompany.mailapp.service.mail;

import com.yourcompany.mailapp.entity.*;
import com.yourcompany.mailapp.repository.*;
import com.yourcompany.mailapp.util.FileUtil;
import com.yourcompany.mailapp.util.MailParser;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailReceiveService {

    private final MailAccountRepository mailAccountRepository;
    private final MailRepository mailRepository;
    private final FolderRepository folderRepository;

    private final MailParser mailParser;
    private final FileUtil fileUtil;

    @Transactional
    public List<Mail> fetchMails(Long mailAccountId) {
        MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                .orElseThrow(() -> new RuntimeException("Mail account not found"));

        List<Mail> newMails = new ArrayList<>();

        try {
            Session session = createImapSession(mailAccount);
            Store store = session.getStore();
            store.connect(mailAccount.getImapHost(), mailAccount.getEmailAddress(), mailAccount.getPassword());

            // Fetch from INBOX
            jakarta.mail.Folder inbox = store.getFolder("INBOX");
            inbox.open(jakarta.mail.Folder.READ_ONLY);

            Folder inboxFolder = folderRepository.findByMailAccountAndType(mailAccount, Folder.FolderType.INBOX)
                    .orElseGet(() -> createFolder(mailAccount, "INBOX", Folder.FolderType.INBOX));

            Message[] messages = inbox.getMessages();
            log.info("Found {} messages in INBOX", messages.length);

            for (Message message : messages) {
                try {
                    String messageId = mailParser.getMessageId(message);

                    // Check if message already exists
                    if (messageId != null && mailRepository.findByMessageId(messageId).isPresent()) {
                        continue;
                    }

                    Mail mail = convertMessageToMail(message, mailAccount, inboxFolder);
                    mailRepository.save(mail);
                    newMails.add(mail);

                } catch (Exception e) {
                    log.error("Error processing message", e);
                }
            }

            inbox.close(false);
            store.close();

            // Update last sync time
            mailAccount.setLastSync(LocalDateTime.now());
            mailAccountRepository.save(mailAccount);

            log.info("Fetched {} new mails for account: {}", newMails.size(), mailAccount.getEmailAddress());

        } catch (Exception e) {
            log.error("Error fetching mails", e);
            throw new RuntimeException("Failed to fetch mails: " + e.getMessage());
        }

        return newMails;
    }

    // readInbox method removed (legacy/hardcoded)

    private Session createImapSession(MailAccount mailAccount) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", mailAccount.getImapUseSsl() ? "imaps" : "imap");
        props.setProperty("mail.imap.host", mailAccount.getImapHost());
        props.setProperty("mail.imap.port", mailAccount.getImapPort().toString());
        props.setProperty("mail.imap.ssl.enable", mailAccount.getImapUseSsl().toString());
        props.setProperty("mail.imap.starttls.enable", "true");

        return Session.getInstance(props);
    }

    private Mail convertMessageToMail(Message message, MailAccount mailAccount, Folder folder) throws Exception {
        Mail mail = new Mail();

        mail.setUid(mailParser.getMessageId(message));
        mail.setMessageId(mailParser.getMessageId(message));
        mail.setFromAddress(mailParser.getFromAddress(message));
        mail.setFromName(mailParser.getFromName(message));
        mail.setToAddresses(mailParser.getToAddresses(message));
        mail.setCcAddresses(mailParser.getCcAddresses(message));
        mail.setSubject(mailParser.getSubject(message));
        mail.setBodyText(mailParser.getTextContent(message));
        mail.setBodyHtml(mailParser.getHtmlContent(message));

        if (message.getSentDate() != null) {
            mail.setSentDate(LocalDateTime.ofInstant(message.getSentDate().toInstant(), ZoneId.systemDefault()));
        }
        if (message.getReceivedDate() != null) {
            mail.setReceivedDate(
                    LocalDateTime.ofInstant(message.getReceivedDate().toInstant(), ZoneId.systemDefault()));
        }

        mail.setIsRead(message.isSet(Flags.Flag.SEEN));
        mail.setIsStarred(message.isSet(Flags.Flag.FLAGGED));
        mail.setSizeBytes((long) mailParser.getMessageSize(message));
        mail.setMailAccount(mailAccount);
        mail.setFolder(folder);
        mail.setInReplyTo(mailParser.getInReplyTo(message));
        mail.setReferences(mailParser.getReferences(message));

        // Handle attachments
        List<Part> attachmentParts = mailParser.getAttachments(message);
        if (!attachmentParts.isEmpty()) {
            mail.setHasAttachments(true);
            for (Part part : attachmentParts) {
                try {
                    String filename = part.getFileName();
                    if (filename != null) {
                        String filePath = fileUtil.saveFile(part.getInputStream(), filename);

                        Attachment attachment = new Attachment();
                        attachment.setFilename(filename);
                        attachment.setContentType(part.getContentType());
                        attachment.setSizeBytes((long) part.getSize());
                        attachment.setFilePath(filePath);
                        attachment.setMail(mail);
                        attachment.setIsInline(Part.INLINE.equalsIgnoreCase(part.getDisposition()));

                        mail.getAttachments().add(attachment);
                    }
                } catch (Exception e) {
                    log.error("Error saving attachment", e);
                }
            }
        }

        return mail;
    }

    private Folder createFolder(MailAccount mailAccount, String name, Folder.FolderType type) {
        Folder folder = new Folder();
        folder.setName(name);
        folder.setFullName(name);
        folder.setType(type);
        folder.setMailAccount(mailAccount);
        return folderRepository.save(folder);
    }

    public Page<Mail> getMailsByFolder(Long folderId, Pageable pageable) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return mailRepository.findByFolderAndIsDeletedFalse(folder, pageable);
    }

    public Mail getMailById(Long mailId) {
        return mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));
    }

    @Transactional
    public Mail markAsRead(Long mailId, boolean read) {
        Mail mail = getMailById(mailId);
        mail.setIsRead(read);
        return mailRepository.save(mail);
    }

    @Transactional
    public Mail markAsStarred(Long mailId, boolean starred) {
        Mail mail = getMailById(mailId);
        mail.setIsStarred(starred);
        return mailRepository.save(mail);
    }

    @Transactional
    public void deleteMail(Long mailId) {
        Mail mail = getMailById(mailId);
        mail.setIsDeleted(true);
        mailRepository.save(mail);
    }

    public Page<Mail> searchMails(Long mailAccountId, String keyword, Pageable pageable) {
        MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                .orElseThrow(() -> new RuntimeException("Mail account not found"));
        return mailRepository.searchMails(mailAccount, keyword, pageable);
    }

    public Page<Mail> advancedSearch(Long mailAccountId, String keyword, String fromAddress,
            Boolean hasAttachments, Boolean isRead, Pageable pageable) {

        MailAccount mailAccount = mailAccountRepository.findById(mailAccountId)
                .orElseThrow(() -> new RuntimeException("Mail account not found"));

        Specification<Mail> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by account
            predicates.add(cb.equal(root.get("mailAccount"), mailAccount));

            // Not deleted
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // Filter by keyword (subject or body)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("subject")), pattern),
                        cb.like(cb.lower(root.get("bodyText")), pattern)));
            }

            // Filter by from address
            if (fromAddress != null && !fromAddress.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("fromAddress")), "%" + fromAddress.toLowerCase() + "%"));
            }

            // Filter by attachments
            if (hasAttachments != null) {
                predicates.add(cb.equal(root.get("hasAttachments"), hasAttachments));
            }

            // Filter by read status
            if (isRead != null) {
                predicates.add(cb.equal(root.get("isRead"), isRead));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return mailRepository.findAll(spec, pageable);
    }

    public Long getUnreadCount(Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return mailRepository.countByFolderAndIsReadFalseAndIsDeletedFalse(folder);
    }
}