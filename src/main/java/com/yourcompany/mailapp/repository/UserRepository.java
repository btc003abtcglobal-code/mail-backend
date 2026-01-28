package com.yourcompany.mailapp.repository;

import com.yourcompany.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // --- Basic lookups (for login & registration) ---

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    // --- Status & role based queries ---

    List<User> findByActiveTrue();

    List<User> findByActiveFalse();

    List<User> findByRole(User.Role role);

    // --- Simple name search ---

    List<User> findByFirstNameIgnoreCase(String firstName);

    List<User> findByLastNameIgnoreCase(String lastName);

    List<User> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    // --- Counts (for dashboard/statistics) ---

    Long countByActiveTrue();

    Long countByActiveFalse();

    Long countByRole(User.Role role);
}