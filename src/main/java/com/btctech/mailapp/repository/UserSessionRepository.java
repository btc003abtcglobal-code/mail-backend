package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByJwtToken(String jwtToken);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
