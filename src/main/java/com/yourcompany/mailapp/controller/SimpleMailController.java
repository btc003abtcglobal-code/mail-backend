package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.service.SimpleMailService;
import com.yourcompany.mailapp.dto.MailRequest;
// import com.yourcompany.mailapp.dto.MailResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/mail")
public class SimpleMailController {

    @Autowired
    private SimpleMailService service;

    @Autowired
    private UserService userService;

    // @GetMapping("/send")
    // public String sendMail() {
    // try {
    // service.sendMail();
    // return "Mail Sent Successfully";
    // } catch (Exception e) {
    // e.printStackTrace();
    // return "Error sending mail: " + e.getMessage();
    // }
    // }

    @PostMapping("/send-mail")
    public String sendMail(@RequestBody MailRequest req, @AuthenticationPrincipal UserDetails userDetails) {
        String from = userDetails != null ? userDetails.getUsername() : "anonymous";
        service.sendMail(from, req.getTo(), req.getSubject(), req.getMessage());
        return "Mail Sent Successfully";
    }

    @PostMapping(value = "/send-attachment", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public org.springframework.http.ResponseEntity<String> sendAttachment(
            @org.springframework.web.bind.annotation.RequestParam("to") String to,
            @org.springframework.web.bind.annotation.RequestParam("subject") String subject,
            @org.springframework.web.bind.annotation.RequestParam("message") String message,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String from = userDetails != null ? userDetails.getUsername() : "anonymous";
            service.sendMailWithAttachment(from, to, subject, message, file);
            return org.springframework.http.ResponseEntity.ok("Mail with attachment sent");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/read-mails")
    public org.springframework.http.ResponseEntity<?> readMails(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return org.springframework.http.ResponseEntity.status(401).body("User must be logged in");
            }
            User user = userService.getUserByUsername(userDetails.getUsername());
            String password = user.getMailPassword();

            // If password is null (legacy users), fallback or error?
            // For now, let's assume all users interacting here have mailPassword set or
            // rely on exception
            if (password == null) {
                return org.springframework.http.ResponseEntity.status(400)
                        .body("User does not have a mail password set.");
            }

            return org.springframework.http.ResponseEntity.ok(service.fetchEmails(user.getUsername(), password));
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.status(500).body("Error reading mails: " + e.getMessage());
        }
    }
}
