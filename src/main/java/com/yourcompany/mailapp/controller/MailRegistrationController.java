package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.dto.MailUserRequest;
import com.yourcompany.mailapp.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailRegistrationController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<String> createMailUser(@RequestBody MailUserRequest request) {
        log.info("Received request to create mail user: {}", request.getUsername());
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("Mail account created successfully");
        } catch (Exception e) {
            log.error("Failed to create mail user", e);
            return ResponseEntity.badRequest().body("Failed to create mail account: " + e.getMessage());
        }
    }
}
