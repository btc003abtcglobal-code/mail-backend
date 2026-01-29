package com.yourcompany.mailapp.config;

import com.yourcompany.mailapp.entity.Folder;
import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.repository.FolderRepository;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import com.yourcompany.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final MailAccountRepository mailAccountRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.mail.host:localhost}")
    private String smtpHost;

    @Value("${spring.mail.port:25}")
    private Integer smtpPort;

    @Value("${imap.host:localhost}")
    private String imapHost;

    @Value("${imap.port:143}")
    private Integer imapPort;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.count() == 0) {
                log.info("Initializing demo data...");

                // Create User
                User user = new User();
                user.setUsername("user");
                user.setEmail("user@localhost");
                user.setPassword(passwordEncoder.encode("password"));
                user.setFirstName("Demo");
                user.setLastName("User");
                user.setRole(User.Role.USER);
                user.setActive(true);
                
                user = userRepository.save(user);
                log.info("Created demo user: user/password");

                // Create Mail Account
                MailAccount account = new MailAccount();
                account.setUser(user);
                account.setEmailAddress("user@localhost");
                account.setDisplayName("Demo User");
                account.setSmtpHost(smtpHost);
                account.setSmtpPort(smtpPort);
                account.setImapHost(imapHost);
                account.setImapPort(imapPort);
                account.setPassword("password"); // Postfix/Dovecot might ignore this if configured so
                account.setSmtpUseTls(false);
                account.setImapUseSsl(false);
                account.setIsDefault(true);
                account.setActive(true);
                account.setLastSync(LocalDateTime.now());

                account = mailAccountRepository.save(account);
                log.info("Created mail account for: {}", account.getEmailAddress());

                // Create default folders
                createFolder(account, "INBOX", Folder.FolderType.INBOX);
                createFolder(account, "Sent", Folder.FolderType.SENT);
                createFolder(account, "Drafts", Folder.FolderType.DRAFTS);
                createFolder(account, "Trash", Folder.FolderType.TRASH);
                createFolder(account, "Spam", Folder.FolderType.SPAM);
                
                log.info("Initialized default folders");
            }
        };
    }

    private void createFolder(MailAccount account, String name, Folder.FolderType type) {
        Folder folder = new Folder();
        folder.setMailAccount(account);
        folder.setName(name);
        folder.setFullName(name);
        folder.setType(type);
        folderRepository.save(folder);
    }
}
