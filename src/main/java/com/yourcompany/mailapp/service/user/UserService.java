package com.yourcompany.mailapp.service.user;

import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import com.yourcompany.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MailAccountRepository mailAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(String username, Map<String, String> updates) {
        User user = getUserByUsername(username);

        if (updates.containsKey("firstName")) {
            user.setFirstName(updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            user.setLastName(updates.get("lastName"));
        }
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(newEmail);
        }

        log.info("User updated: {}", username);
        return java.util.Objects.requireNonNull(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = getUserByUsername(username);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", username);
    }

    @Transactional
    public MailAccount addMailAccount(String username, MailAccount mailAccount) {
        User user = getUserByUsername(username);

        mailAccount.setUser(user);

        // If this is the first account, make it default
        List<MailAccount> existingAccounts = mailAccountRepository.findByUser(user);
        if (existingAccounts.isEmpty()) {
            mailAccount.setIsDefault(true);
        }

        log.info("Mail account added for user: {}", username);
        return mailAccountRepository.save(mailAccount);
    }

    public List<Map<String, Object>> getMailAccounts(String username) {
        User user = getUserByUsername(username);
        List<MailAccount> accounts = mailAccountRepository.findByUser(user);

        return accounts.stream()
                .map(this::mapMailAccountToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMailAccount(String username, Long accountId) {
        User user = getUserByUsername(username);
        MailAccount account = mailAccountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Mail account not found"));

        mailAccountRepository.delete(account);
        log.info("Mail account deleted: {} for user: {}", accountId, username);
    }

    @Transactional
    public void setDefaultMailAccount(String username, Long accountId) {
        User user = getUserByUsername(username);

        // Remove default from all accounts
        List<MailAccount> accounts = mailAccountRepository.findByUser(user);
        accounts.forEach(acc -> acc.setIsDefault(false));
        mailAccountRepository.saveAll(accounts);

        // Set new default
        MailAccount account = mailAccountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Mail account not found"));
        account.setIsDefault(true);
        mailAccountRepository.save(account);

        log.info("Default mail account set: {} for user: {}", accountId, username);
    }

    private Map<String, Object> mapMailAccountToResponse(MailAccount account) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", account.getId());
        response.put("emailAddress", account.getEmailAddress());
        response.put("displayName", account.getDisplayName());
        response.put("smtpHost", account.getSmtpHost());
        response.put("smtpPort", account.getSmtpPort());
        response.put("imapHost", account.getImapHost());
        response.put("imapPort", account.getImapPort());
        response.put("isDefault", account.getIsDefault());
        response.put("active", account.getActive());
        response.put("lastSync", account.getLastSync());
        return response;
    }

    // @Transactional // Removed to prevent DB deadlock during long-running system
    // user creation
    public void registerUser(com.yourcompany.mailapp.dto.MailUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getUsername() + "@yourmail.local")) {
            throw new RuntimeException("Email already exists");
        }

        // 1. Save to DB (Transactional by default in Repository)
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMailPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAccountType(request.getAccountType());

        String domain = "yourmail.local";
        user.setEmail(request.getUsername() + "@" + domain);
        user.setRole(User.Role.USER);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        // 2. Create System User (Linux + Postfix/Dovecot) - OUTSIDE DB TRANSACTION
        try {
            createSystemUser(request.getUsername(), request.getPassword());
            log.info("User registered successfully: {}", request.getUsername());
        } catch (Exception e) {
            log.error("System user creation failed. Rolling back DB user: {}", request.getUsername());
            userRepository.delete(savedUser); // Manual rollback
            throw new RuntimeException("Failed to register user. System error: " + e.getMessage());
        }
    }

    public void createSystemUser(String username, String password) {
        try {
            log.info("Creating system user: {}", username);

            // 1. Create Linux user
            String[] createUserCmd = { "sudo", "useradd", "-m", username };
            ProcessBuilder pbUser = new ProcessBuilder(createUserCmd);
            pbUser.inheritIO();
            Process pUser = pbUser.start();
            pUser.waitFor();

            // 2. Set password (important for SASL auth)
            String[] setPassCmd = { "/bin/sh", "-c", "echo " + username + ":" + password + " | sudo chpasswd" };
            ProcessBuilder pbPass = new ProcessBuilder(setPassCmd);
            pbPass.inheritIO();
            Process pPass = pbPass.start();
            pPass.waitFor();

            // 3. Create mailbox
            String mailboxPath = "/var/mail/" + username;
            String[] touchCmd = { "sudo", "touch", mailboxPath };
            new ProcessBuilder(touchCmd).start().waitFor();

            // 4. Set permissions
            String[] chownCmd = { "sudo", "chown", username + ":mail", mailboxPath };
            new ProcessBuilder(chownCmd).start().waitFor();

            log.info("System user created successfully: {}", username);

        } catch (Exception e) {
            log.error("Failed to create system user: " + username, e);
            // Non-blocking for now, or throw? better to throw to fail transaction if
            // possible,
            // but checked exceptions inside lambda or complex logic.
            // Since we are in @Transactional, throwing RuntimeException will rollback DB
            // save.
            throw new RuntimeException("Failed to create mail account: " + e.getMessage());
        }
    }
}
