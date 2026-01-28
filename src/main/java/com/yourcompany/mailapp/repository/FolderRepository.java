package com.yourcompany.mailapp.repository;

import com.yourcompany.mailapp.entity.Folder;
import com.yourcompany.mailapp.entity.MailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    List<Folder> findByMailAccount(MailAccount mailAccount);
    
    Optional<Folder> findByMailAccountAndType(MailAccount mailAccount, Folder.FolderType type);
    
    Optional<Folder> findByMailAccountAndFullName(MailAccount mailAccount, String fullName);
    
    List<Folder> findByMailAccountId(Long mailAccountId);
}
