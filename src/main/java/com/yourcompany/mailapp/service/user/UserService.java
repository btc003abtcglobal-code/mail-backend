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
        return userRepository.save(user);
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
}
