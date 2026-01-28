package com.yourcompany.mailapp.repository;

import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

    // ---------- Basic user-related ----------

    List<MailAccount> findByUser(User user);

    List<MailAccount> findByUserId(Long userId);

    Optional<MailAccount> findByIdAndUserId(Long id, Long userId);

    Optional<MailAccount> findByEmailAddressAndUser(String emailAddress, User user);

    Boolean existsByEmailAddressAndUserId(String emailAddress, Long userId);


    // ---------- Default account ----------

    Optional<MailAccount> findByUserAndIsDefaultTrue(User user);

    Optional<MailAccount> findByUserIdAndIsDefaultTrue(Long userId);

    Long countByUserIdAndIsDefaultTrue(Long userId);


    // ---------- Active / inactive ----------

    List<MailAccount> findByUserAndActiveTrue(User user);

    List<MailAccount> findByUserIdAndActiveTrue(Long userId);

    Long countByUserIdAndActiveTrue(Long userId);

    Long countByUserIdAndActiveFalse(Long userId);


    // ---------- Email address ----------

    Optional<MailAccount> findByEmailAddress(String emailAddress);

    Boolean existsByEmailAddress(String emailAddress);


    // ---------- Sync ----------

    @Query("SELECT ma FROM MailAccount ma WHERE ma.active = true " +
           "AND (ma.lastSync IS NULL OR ma.lastSync < :date)")
    List<MailAccount> findAccountsNeedingSync(@Param("date") LocalDateTime date);


    // ---------- Bulk operations ----------

    @Modifying
    @Query("UPDATE MailAccount ma SET ma.lastSync = :syncTime WHERE ma.id = :accountId")
    int updateLastSync(@Param("accountId") Long accountId,
                       @Param("syncTime") LocalDateTime syncTime);

    @Modifying
    @Query("UPDATE MailAccount ma SET ma.isDefault = " +
           "CASE WHEN ma.id = :accountId THEN true ELSE false END " +
           "WHERE ma.user.id = :userId")
    int setDefaultAccount(@Param("userId") Long userId,
                          @Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE MailAccount ma SET ma.active = false WHERE ma.id = :accountId")
    int deactivateAccount(@Param("accountId") Long accountId);
}
