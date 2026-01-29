package com.yourcompany.mailapp.service.mail;

import com.yourcompany.mailapp.config.MailConfig;
import com.yourcompany.mailapp.dto.SendMailRequest;
import com.yourcompany.mailapp.entity.Attachment;
import com.yourcompany.mailapp.entity.Folder;
import com.yourcompany.mailapp.entity.Mail;
import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.repository.AttachmentRepository;
import com.yourcompany.mailapp.repository.FolderRepository;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import com.yourcompany.mailapp.repository.MailRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSendService {

    private final MailAccountRepository mailAccountRepository;
    private final MailRepository mailRepository;
    private final FolderRepository folderRepository;
    private final AttachmentRepository attachmentRepository;
    private final MailConfig mailConfig;

    @Transactional
    public Mail sendMail(SendMailRequest request) {
        try {
            // Get mail account
            MailAccount mailAccount = mailAccountRepository.findById(request.getMailAccountId())
                    .orElseThrow(() -> new RuntimeException("Mail account not found"));

            // Determine if we should use auth. For port 25 (local), we usually don't use
            // auth.
            String smtpUsername = mailAccount.getEmailAddress();
            String smtpPassword = mailAccount.getPassword();
            if (mailAccount.getSmtpPort() == 25) {
                smtpUsername = null;
                smtpPassword = null;
            }

            // Create custom mail sender for this account
            JavaMailSender mailSender = mailConfig.createMailSender(
                    mailAccount.getSmtpHost(),
                    mailAccount.getSmtpPort(),
                    smtpUsername,
                    smtpPassword,
                    mailAccount.getSmtpUseTls());

            // Create message
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailAccount.getEmailAddress(), mailAccount.getDisplayName());
            helper.setTo(request.getTo().split(","));

            if (request.getCc() != null && !request.getCc().isEmpty()) {
                helper.setCc(request.getCc().split(","));
            }

            if (request.getBcc() != null && !request.getBcc().isEmpty()) {
                helper.setBcc(request.getBcc().split(","));
            }

            helper.setSubject(request.getSubject());

            String bodyText = request.getBodyText() != null ? request.getBodyText() : "";

            if (request.getBodyHtml() != null && !request.getBodyHtml().isEmpty()) {
                helper.setText(bodyText, request.getBodyHtml());
            } else {
                helper.setText(bodyText);
            }

            // Add attachments
            if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
                List<Attachment> attachments = attachmentRepository.findAllById(request.getAttachmentIds());
                for (Attachment attachment : attachments) {
                    FileSystemResource file = new FileSystemResource(new File(attachment.getFilePath()));
                    helper.addAttachment(attachment.getFilename(), file);
                }
            }

            // Set headers for reply
            if (request.getInReplyTo() != null) {
                mimeMessage.setHeader("In-Reply-To", request.getInReplyTo());
            }
            if (request.getReferences() != null) {
                mimeMessage.setHeader("References", request.getReferences());
            }

            // Send mail
            mailSender.send(mimeMessage);
            log.info("Mail sent successfully from: {}", mailAccount.getEmailAddress());

            // Save to Sent folder
            Folder sentFolder = folderRepository.findByMailAccountAndType(mailAccount, Folder.FolderType.SENT)
                    .orElseThrow(() -> new RuntimeException("Sent folder not found"));

            Mail mail = new Mail();
            mail.setUid(UUID.randomUUID().toString());
            mail.setMessageId(mimeMessage.getMessageID());
            mail.setFromAddress(mailAccount.getEmailAddress());
            mail.setFromName(mailAccount.getDisplayName());
            mail.setToAddresses(request.getTo());
            mail.setCcAddresses(request.getCc());
            mail.setBccAddresses(request.getBcc());
            mail.setSubject(request.getSubject());
            mail.setBodyText(request.getBodyText());
            mail.setBodyHtml(request.getBodyHtml());
            mail.setSentDate(LocalDateTime.now());
            mail.setReceivedDate(LocalDateTime.now());
            mail.setIsRead(true);
            mail.setHasAttachments(request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty());
            mail.setMailAccount(mailAccount);
            mail.setFolder(sentFolder);
            mail.setInReplyTo(request.getInReplyTo());
            mail.setReferences(request.getReferences());

            return mailRepository.save(mail);

        } catch (Exception e) {
            log.error("Error sending mail", e);
            throw new RuntimeException("Failed to send mail: " + e.getMessage());
        }
    }

    @Transactional
    public Mail forwardMail(Long mailId, SendMailRequest request) {
        Mail originalMail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Original mail not found"));

        request.setSubject("Fwd: " + originalMail.getSubject());
        request.setBodyText("---------- Forwarded message ---------\n" +
                "From: " + originalMail.getFromName() + " <" + originalMail.getFromAddress() + ">\n" +
                "Date: " + originalMail.getSentDate() + "\n" +
                "Subject: " + originalMail.getSubject() + "\n\n" +
                originalMail.getBodyText());

        return sendMail(request);
    }

    @Transactional
    public Mail replyMail(Long mailId, SendMailRequest request) {
        Mail originalMail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Original mail not found"));

        request.setTo(originalMail.getFromAddress());
        request.setSubject("Re: " + originalMail.getSubject().replaceFirst("^Re: ", ""));
        request.setInReplyTo(originalMail.getMessageId());
        request.setReferences(originalMail.getMessageId());

        return sendMail(request);
    }
}
