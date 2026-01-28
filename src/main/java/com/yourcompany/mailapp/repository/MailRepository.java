package com.yourcompany.mailapp.repository;

import com.yourcompany.mailapp.entity.Folder;
import com.yourcompany.mailapp.entity.Mail;
import com.yourcompany.mailapp.entity.MailAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailRepository extends JpaRepository<Mail, Long>, JpaSpecificationExecutor<Mail> {

        // ---------- Basic lookup ----------

        Optional<Mail> findByUid(String uid);

        Optional<Mail> findByMessageId(String messageId);

        Boolean existsByUid(String uid);

        // ---------- Folder based ----------

        Page<Mail> findByFolderAndIsDeletedFalse(Folder folder, Pageable pageable);

        Long countByFolderAndIsDeletedFalse(Folder folder);

        Long countByFolderAndIsReadFalseAndIsDeletedFalse(Folder folder);

        // ---------- Mail account based ----------

        Page<Mail> findByMailAccountAndIsDeletedFalse(MailAccount mailAccount, Pageable pageable);

        Long countByMailAccountAndIsDeletedFalse(MailAccount mailAccount);

        Long countByMailAccountAndIsReadFalseAndIsDeletedFalse(MailAccount mailAccount);

        // ---------- Read / Unread ----------

        Page<Mail> findByFolderAndIsReadFalseAndIsDeletedFalse(Folder folder, Pageable pageable);

        Page<Mail> findByMailAccountAndIsReadFalseAndIsDeletedFalse(MailAccount mailAccount, Pageable pageable);

        // ---------- Starred ----------

        Page<Mail> findByMailAccountAndIsStarredTrueAndIsDeletedFalse(MailAccount mailAccount, Pageable pageable);

        // ---------- Simple search ----------

        @Query("SELECT m FROM Mail m WHERE m.mailAccount = :mailAccount " +
                        "AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(m.fromAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND m.isDeleted = false")
        Page<Mail> searchMails(@Param("mailAccount") MailAccount mailAccount,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        // ---------- Date based ----------

        Page<Mail> findByMailAccountAndReceivedDateAfterAndIsDeletedFalse(
                        MailAccount mailAccount, LocalDateTime date, Pageable pageable);

        // ---------- Bulk operations ----------

        @Modifying
        @Query("UPDATE Mail m SET m.isRead = true WHERE m.folder.id = :folderId AND m.isDeleted = false")
        int markAllAsReadInFolder(@Param("folderId") Long folderId);

        @Modifying
        @Query("UPDATE Mail m SET m.isDeleted = true WHERE m.id IN :mailIds")
        int softDeleteMails(@Param("mailIds") List<Long> mailIds);

        @Query("SELECT COUNT(m), COUNT(CASE WHEN m.isRead = false THEN 1 END) " +
                        "FROM Mail m " +
                        "WHERE m.folder.id = :folderId AND m.isDeleted = false")
        Object[] getFolderStatistics(@Param("folderId") Long folderId);
}
