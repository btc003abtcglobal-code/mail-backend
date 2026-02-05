package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.UserSession;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessionRepository;

    // AES encryption key (in production, use environment variable)
    private static final String ENCRYPTION_KEY = "BtcTech2026Key16"; // 16 bytes for AES-128

    /**
     * Create session with encrypted password
     */
    @Transactional
    public UserSession createSession(Long userId, Long mailAccountId,
            String password, String jwtToken) {
        try {
            // Encrypt password
            String encryptedPassword = encrypt(password);

            // Create session
            UserSession session = new UserSession();
            session.setUserId(userId);
            session.setMailAccountId(mailAccountId);
            session.setEncryptedPassword(encryptedPassword);
            session.setJwtToken(jwtToken);
            session.setExpiresAt(LocalDateTime.now().plusDays(1)); // 24 hours

            session = sessionRepository.save(session);
            log.info("Created session for user: {}", userId);

            return session;

        } catch (Exception e) {
            log.error("Failed to create session", e);
            throw new MailException("Failed to create session");
        }
    }

    /**
     * Get password from session
     */
    public String getPasswordFromSession(String jwtToken) {
        try {
            UserSession session = sessionRepository.findByJwtToken(jwtToken)
                    .orElseThrow(() -> new MailException("Session not found"));

            // Check expiry
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new MailException("Session expired");
            }

            // Decrypt password
            return decrypt(session.getEncryptedPassword());

        } catch (Exception e) {
            log.error("Failed to get password from session", e);
            throw new MailException("Failed to retrieve password");
        }
    }

    /**
     * Encrypt password
     */
    private String encrypt(String plainText) throws Exception {
        SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypt password
     */
    private String decrypt(String encryptedText) throws Exception {
        SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted);
    }

    /**
     * Delete expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up expired sessions");
    }
}
