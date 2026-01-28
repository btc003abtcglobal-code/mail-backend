package com.yourcompany.mailapp.service.admin;

import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import com.yourcompany.mailapp.repository.MailRepository;
import com.yourcompany.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainService {
    
    private final UserRepository userRepository;
    private final MailAccountRepository mailAccountRepository;
    private final MailRepository mailRepository;
    
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::mapUserToResponse)
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalMailAccounts", mailAccountRepository.count());
        stats.put("totalMails", mailRepository.count());
        stats.put("activeUsers", userRepository.findAll().stream().filter(User::getActive).count());
        return stats;
    }
    
    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.getActive());
        log.info("User status toggled: {} - Active: {}", user.getUsername(), user.getActive());
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }
    
    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole().name());
        response.put("active", user.getActive());
        response.put("createdAt", user.getCreatedAt());
        response.put("mailAccountCount", user.getMailAccounts().size());
        return response;
    }
}

