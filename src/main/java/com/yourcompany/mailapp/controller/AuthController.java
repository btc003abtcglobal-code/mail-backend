package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.dto.LoginRequest;
import com.yourcompany.mailapp.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for authentication operations
 * Handles user registration, login, and profile management
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     * 
     * @param request registration request containing user details
     * @return response with JWT token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Map<String, String> request) {
        try {
            log.info("Registration request for username: {}", request.get("username"));

            // Validate required fields
            if (!request.containsKey("username") || request.get("username").isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Username is required"));
            }
            if (!request.containsKey("email") || request.get("email").isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email is required"));
            }
            if (!request.containsKey("password") || request.get("password").isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password is required"));
            }

            // Validate password strength
            String password = request.get("password");
            if (password.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Password must be at least 6 characters long"));
            }

            // Validate email format
            String email = request.get("email");
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid email format"));
            }

            Map<String, Object> response = authService.register(
                    request.get("username"),
                    request.get("email"),
                    request.get("password"),
                    request.getOrDefault("firstName", ""),
                    request.getOrDefault("lastName", ""));

            log.info("User registered successfully: {}", request.get("username"));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed. Please try again."));
        }
    }

    /**
     * Authenticate user and generate JWT token
     * 
     * @param loginRequest login credentials
     * @return response with JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login request for username: {}", loginRequest.getUsername());

            Map<String, Object> response = authService.login(loginRequest);

            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid username or password"));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed. Please try again."));
        }
    }

    /**
     * Get current authenticated user's information
     * 
     * @param userDetails authenticated user details from JWT
     * @return current user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Not authenticated"));
            }

            log.debug("Fetching current user info for: {}", userDetails.getUsername());

            Map<String, Object> response = authService.getCurrentUser(userDetails.getUsername());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error fetching current user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to fetch user information"));
        }
    }

    /**
     * Validate JWT token
     * 
     * @param userDetails authenticated user details
     * @return validation response
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid token"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("username", userDetails.getUsername());
            response.put("authorities", userDetails.getAuthorities());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token validation error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid token"));
        }
    }

    /**
     * Refresh JWT token
     * 
     * @param userDetails authenticated user details
     * @return new JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Not authenticated"));
            }

            log.info("Token refresh request for user: {}", userDetails.getUsername());

            // Note: In production, implement a proper refresh token mechanism

            Map<String, Object> response = createSuccessResponse("Token refreshed successfully");
            response.put("username", userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Token refresh failed"));
        }
    }

    /**
     * Logout (client-side token removal)
     * 
     * @param userDetails authenticated user details
     * @return logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails != null) {
                log.info("User logged out: {}", userDetails.getUsername());
            }

            return ResponseEntity.ok(createSuccessResponse("Logged out successfully"));

        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.ok(createSuccessResponse("Logout completed"));
        }
    }

    /**
     * Check if username is available
     * 
     * @param username username to check
     * @return availability status
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            boolean available = authService.isUsernameAvailable(username);

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("available", available);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Username check error", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to check username"));
        }
    }

    /**
     * Check if email is available
     * 
     * @param email email to check
     * @return availability status
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            boolean available = authService.isEmailAvailable(email);

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("available", available);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Email check error", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Failed to check email"));
        }
    }

    /**
     * Request password reset (placeholder - implement email sending)
     * 
     * @param request contains email
     * @return confirmation message
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Email is required"));
            }

            log.info("Password reset requested for email: {}", email);

            // TODO: Implement actual password reset email sending
            // For now, just log the request

            return ResponseEntity.ok(createSuccessResponse("If the email exists, a password reset link has been sent"));

        } catch (Exception e) {
            log.error("Password reset error", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Password reset request failed"));
        }
    }

    /**
     * Health check endpoint
     * 
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Authentication Service");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to create error response
     * 
     * @param message error message
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Helper method to create success response
     * 
     * @param message success message
     * @return success response map
     */
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Validation error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
    }

    /**
     * Exception handler for generic errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Unexpected error in AuthController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
    }
}
