package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.entity.User;
import com.yourcompany.mailapp.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // -------------------- Get User by Username --------------------

    @GetMapping("/by-username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    // -------------------- Get User by ID --------------------

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // -------------------- Update User Profile --------------------

    /**
     * Update firstName, lastName, email
     * Body example:
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com"
     * }
     */
    @PutMapping("/{username}")
    public ResponseEntity<User> updateUser(
            @PathVariable String username,
            @RequestBody Map<String, String> updates) {

        User updated = userService.updateUser(username, updates);
        return ResponseEntity.ok(updated);
    }

    // -------------------- Change Password --------------------

    /**
     * Body example:
     * {
     *   "oldPassword": "old123",
     *   "newPassword": "new123"
     * }
     */
    @PostMapping("/{username}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String username,
            @RequestBody Map<String, String> body) {

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        userService.changePassword(username, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    // -------------------- Add Mail Account --------------------

    @PostMapping("/{username}/mail-accounts")
    public ResponseEntity<MailAccount> addMailAccount(
            @PathVariable String username,
            @RequestBody MailAccount mailAccount) {

        MailAccount created = userService.addMailAccount(username, mailAccount);
        return ResponseEntity.ok(created);
    }

    // -------------------- List Mail Accounts --------------------

    @GetMapping("/{username}/mail-accounts")
    public ResponseEntity<List<Map<String, Object>>> getMailAccounts(
            @PathVariable String username) {

        List<Map<String, Object>> accounts = userService.getMailAccounts(username);
        return ResponseEntity.ok(accounts);
    }

    // -------------------- Delete Mail Account --------------------

    @DeleteMapping("/{username}/mail-accounts/{accountId}")
    public ResponseEntity<Void> deleteMailAccount(
            @PathVariable String username,
            @PathVariable Long accountId) {

        userService.deleteMailAccount(username, accountId);
        return ResponseEntity.noContent().build();
    }

    // -------------------- Set Default Mail Account --------------------

    @PostMapping("/{username}/mail-accounts/{accountId}/set-default")
    public ResponseEntity<Void> setDefaultMailAccount(
            @PathVariable String username,
            @PathVariable Long accountId) {

        userService.setDefaultMailAccount(username, accountId);
        return ResponseEntity.ok().build();
    }
}
