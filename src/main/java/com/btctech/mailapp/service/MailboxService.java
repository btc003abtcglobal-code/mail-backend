package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.CreateEmailRequest;
import com.btctech.mailapp.entity.Domain;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.DomainRepository;
import com.btctech.mailapp.repository.MailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxService {

    private final MailAccountRepository mailAccountRepository;
    private final DomainRepository domainRepository;

    @Value("${mail.domain}")
    private String mailDomain;

    @Value("${mail.storage.base-path}")
    private String basePath;

    /**
     * Create custom email for user
     */
    @Transactional
    public MailAccount createCustomEmail(User user, CreateEmailRequest request) {
        try {
            // Validate email name
            validateEmailName(request.getEmailName());

            // Get domain
            Domain domain = domainRepository.findByDomain(mailDomain)
                    .orElseThrow(() -> new MailException("Domain not found"));

            String fullEmail = request.getEmailName() + "@" + mailDomain;

            // Check if email exists
            if (mailAccountRepository.existsByEmail(fullEmail)) {
                throw new MailException("Email already exists");
            }

            // Create maildir structure
            String maildirPath = createMaildirStructure(request.getEmailName());

            // Create mail account record
            MailAccount mailAccount = new MailAccount();
            mailAccount.setUserId(user.getId());
            mailAccount.setDomainId(domain.getId());
            mailAccount.setEmailName(request.getEmailName());
            mailAccount.setEmail(fullEmail);
            mailAccount.setMaildirPath(maildirPath);
            mailAccount.setIsPrimary(false); // Not primary by default
            mailAccount.setActive(true);

            mailAccount = mailAccountRepository.save(mailAccount);
            log.info("Created custom email: {}", fullEmail);

            return mailAccount;

        } catch (IOException e) {
            log.error("Failed to create mailbox", e);
            throw new MailException("Failed to create mailbox: " + e.getMessage());
        }
    }

    /**
     * Set primary email for user
     */
    @Transactional
    public void setPrimaryEmail(Long userId, Long mailAccountId) {
        // Remove primary from all user's accounts
        List<MailAccount> accounts = mailAccountRepository.findByUserId(userId);
        for (MailAccount account : accounts) {
            account.setIsPrimary(false);
            mailAccountRepository.save(account);
        }

        // Set new primary
        MailAccount primary = mailAccountRepository.findById(mailAccountId)
                .orElseThrow(() -> new MailException("Mail account not found"));

        if (!primary.getUserId().equals(userId)) {
            throw new MailException("Unauthorized");
        }

        primary.setIsPrimary(true);
        mailAccountRepository.save(primary);

        log.info("Set primary email: {} for user: {}", primary.getEmail(), userId);
    }

    /**
     * Get user's email accounts
     */
    public List<MailAccount> getUserEmails(Long userId) {
        return mailAccountRepository.findByUserId(userId);
    }

    /**
     * Get primary email account
     */
    public MailAccount getPrimaryEmail(Long userId) {
        return mailAccountRepository.findByUserIdAndIsPrimary(userId, true)
                .orElseThrow(() -> new MailException("No primary email found"));
    }

    /**
     * Get mail account by email
     */
    public MailAccount getMailAccountByEmail(String email) {
        return mailAccountRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("Mail account not found"));
    }

    /**
     * Validate email name
     */
    private void validateEmailName(String emailName) {
        if (emailName == null || emailName.isEmpty()) {
            throw new MailException("Email name is required");
        }

        if (!emailName.equals(emailName.toLowerCase())) {
            throw new MailException("Email name must be lowercase");
        }

        if (!emailName.matches("^[a-z0-9._-]+$")) {
            throw new MailException("Invalid email name format");
        }
    }

    /**
     * Create Maildir structure
     */
    private String createMaildirStructure(String emailName) throws IOException {
        Path userMailPath = Paths.get(basePath, mailDomain, emailName);
        Path newDir = userMailPath.resolve("new");
        Path curDir = userMailPath.resolve("cur");
        Path tmpDir = userMailPath.resolve("tmp");

        Files.createDirectories(newDir);
        Files.createDirectories(curDir);
        Files.createDirectories(tmpDir);

        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        Files.setPosixFilePermissions(userMailPath, perms);
        Files.setPosixFilePermissions(newDir, perms);
        Files.setPosixFilePermissions(curDir, perms);
        Files.setPosixFilePermissions(tmpDir, perms);

        try {
            Runtime.getRuntime().exec(new String[] {
                    "chown", "-R", "vmail:vmail", userMailPath.toString()
            }).waitFor();
        } catch (Exception e) {
            log.warn("Could not change owner to vmail:vmail - {}", e.getMessage());
        }

        log.info("Created Maildir structure: {}", userMailPath);
        return userMailPath.toString();
    }
}
