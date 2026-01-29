package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.service.admin.DomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DomainService domainService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(domainService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            return ResponseEntity.ok(domainService.getSystemStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId) {
        try {
            domainService.toggleUserStatus(userId);
            return ResponseEntity.ok(Map.of("message", "User status toggled successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            domainService.deleteUser(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private final com.yourcompany.mailapp.repository.UserRepository userRepository;

    @GetMapping("/mail-users")
    public java.util.List<Map<String, String>> getAllMailUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    Map<String, String> map = new java.util.HashMap<>();
                    map.put("username", user.getUsername());
                    map.put("email", user.getEmail());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
