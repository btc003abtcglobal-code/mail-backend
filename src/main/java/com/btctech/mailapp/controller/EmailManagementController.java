package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.CreateEmailRequest;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailManagementController {

    private final MailboxService mailboxService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * STEP 2: Create custom email (uses tempToken from registration)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEmail(
            @Valid @RequestBody CreateEmailRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // Extract token
        String token = authHeader.substring(7);
        String tokenSubject = jwtUtil.extractEmail(token); // Assuming extractEmail extracts the subject (email or
                                                           // temp_username)

        log.info("Create email request: {} by token: {}", request.getEmailName(), tokenSubject);

        // Get user (from temp token or existing email)
        User user;
        if (tokenSubject.startsWith("temp_")) {
            String username = tokenSubject.substring(5); // Remove "temp_"
            user = userService.getUserByUsername(username);
        } else {
            user = userService.getUserByEmail(tokenSubject);
        }

        // Create custom email
        MailAccount mailAccount = mailboxService.createCustomEmail(user, request);

        // Set as primary if first email
        List<MailAccount> existing = mailboxService.getUserEmails(user.getId());
        if (existing.size() == 1) {
            mailboxService.setPrimaryEmail(user.getId(), mailAccount.getId());
        }

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("emailId", mailAccount.getId());
        data.put("email", mailAccount.getEmail());
        data.put("emailName", mailAccount.getEmailName());
        data.put("message", "Email created successfully! You can now login with: " + mailAccount.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(data, "Email created successfully"));
    }

    /**
     * Get all emails for current user
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listEmails(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        List<MailAccount> accounts = mailboxService.getUserEmails(user.getId());

        List<Map<String, Object>> emailList = accounts.stream().map(account -> {
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("id", account.getId());
            emailData.put("emailName", account.getEmailName());
            emailData.put("email", account.getEmail());
            emailData.put("isPrimary", account.getIsPrimary());
            emailData.put("active", account.getActive());
            emailData.put("createdAt", account.getCreatedAt());
            return emailData;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("count", emailList.size());
        data.put("emails", emailList);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Emails retrieved"));
    }

    /**
     * Set primary email
     */
    @PostMapping("/{emailId}/set-primary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setPrimary(
            @PathVariable Long emailId,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        mailboxService.setPrimaryEmail(user.getId(), emailId);

        MailAccount primary = mailboxService.getPrimaryEmail(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("primaryEmail", primary.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(data, "Primary email updated"));
    }
}
