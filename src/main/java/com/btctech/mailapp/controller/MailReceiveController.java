package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.MailResponse;
import com.btctech.mailapp.service.MailReceiveService;
import com.btctech.mailapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailReceiveController {

    private final MailReceiveService mailReceiveService;
    private final SessionService sessionService;

    /**
     * Read inbox - NO PASSWORD NEEDED!
     */
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readInbox(
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        String email = authentication.getName();
        String token = authHeader.substring(7);

        log.info("Read inbox request for: {}", email);

        // Get password from session
        String password = sessionService.getPasswordFromSession(token);

        // Read inbox
        List<MailResponse> mails = mailReceiveService.readInbox(email, password);

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("count", mails.size());
        data.put("mails", mails);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Inbox retrieved successfully"));
    }
}
