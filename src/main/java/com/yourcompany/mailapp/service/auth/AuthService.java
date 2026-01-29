package com.yourcompany.mailapp.service.auth;

import com.yourcompany.mailapp.dto.LoginRequest;
import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.repository.UserRepository;
import com.yourcompany.mailapp.security.JwtUtil;
import com.yourcompany.mailapp.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public Map<String, Object> register(String username, String email, String password, String firstName,
            String lastName) {
        // specific checks just in case, though controller also checks
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setMailPassword(password); // Store cleartext for IMAP/SMTP
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(User.Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);

        User savedUser = userRepository.save(user);

        // Create Linux system user for mail
        userService.createSystemUser(username, password);

        // Auto-login (generate token)
        // We can create a UserDetails object manually since we just created the user
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getUsername())
                .password(savedUser.getPassword())
                .roles(savedUser.getRole().name())
                .build();

        String token = jwtUtil.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", savedUser.getId());
        response.put("username", savedUser.getUsername());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        response.put("firstName", savedUser.getFirstName());
        response.put("lastName", savedUser.getLastName());

        return response;
    }

    public Map<String, Object> login(LoginRequest loginRequest) {
        String usernameOrEmail = loginRequest.getUsername();

        // If username is null/empty, check if email is provided
        if ((usernameOrEmail == null || usernameOrEmail.isEmpty()) && loginRequest.getEmail() != null) {
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + loginRequest.getEmail()));
            usernameOrEmail = user.getUsername();
        }

        if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
            throw new RuntimeException("Username or Email is required");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return response;
    }

    public Map<String, Object> getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("role", user.getRole());
        response.put("createdAt", user.getCreatedAt());

        return response;
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
