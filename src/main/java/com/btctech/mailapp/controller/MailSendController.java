package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.service.MailSendService;
import com.btctech.mailapp.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailSendController {

    private final MailSendService mailSendService;
    private final SessionService sessionService;

    /**
     * Send email - NO PASSWORD NEEDED!
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMail(
            @Valid @RequestBody SendMailRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        String fromEmail = authentication.getName();
        String token = authHeader.substring(7);

        log.info("Send mail request from {} to {}", fromEmail, request.getTo());

        // Get password from session
        String password = sessionService.getPasswordFromSession(token);

        // Send mail
        mailSendService.sendMail(fromEmail, password, request);

        Map<String, Object> data = new HashMap<>();
        data.put("from", fromEmail);
        data.put("to", request.getTo());
        data.put("subject", request.getSubject());
        data.put("sentAt", System.currentTimeMillis());

        return ResponseEntity.ok(
                ApiResponse.success(data, "Email sent successfully"));
    }
}
