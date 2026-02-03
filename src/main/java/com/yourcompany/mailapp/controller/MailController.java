package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.dto.SendMailRequest;
import com.yourcompany.mailapp.service.mail.MailReadService;
import com.yourcompany.mailapp.service.mail.MailSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MailController {

    private final MailSendService mailSendService;
    private final MailReadService mailReadService;

    // 8️⃣ SEND MAIL API
    @PostMapping("/mail/send")
    public ResponseEntity<?> sendMail(
            @RequestBody SendMailRequest req,
            Authentication auth) throws Exception {

        String email = auth.getName(); // from JWT

        // In user snippet 8, it says: String password = req.getPassword(); // or fetch
        // securely
        // SendMailRequest I created has password.
        String password = req.getPassword();

        mailSendService.send(
                email,
                password,
                req.getTo(),
                req.getSubject(),
                req.getBody());

        return ResponseEntity.ok("Mail sent");
    }

    // 🔟 READ MAIL API
    // Note: User snippet 10 has @GetMapping("/mail/inbox")
    @GetMapping("/mail/inbox")
    public List<?> inbox(Authentication auth,
            @RequestParam String password) throws Exception {

        return mailReadService.readInbox(auth.getName(), password);
    }
}