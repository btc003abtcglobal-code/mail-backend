package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.dto.LoginRequest;
import com.yourcompany.mailapp.dto.RegisterRequest;
import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.repository.UserRepository;
import com.yourcompany.mailapp.security.JwtUtil;
import com.yourcompany.mailapp.service.mail.VpsMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final VpsMailService vpsMailService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) throws Exception {

        String email = req.getMailName() + "@btctech.shop";

        if (userRepo.existsByEmail(email))
            return ResponseEntity.status(409).body("Email exists");

        // Create VPS mailbox FIRST
        vpsMailService.createMailbox(req.getMailName(), req.getPassword());

        // Save app user
        User user = new User();
        user.setEmail(email);
        user.setPassword(encoder.encode(req.getPassword()));
        // Note: active is true by default in Entity
        userRepo.save(user);

        return ResponseEntity.ok(Map.of(
                "email", email,
                "status", "CREATED"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid login"));

        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid login");

        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(Map.of("token", token));
    }
}
