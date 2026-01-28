package com.yourcompany.mailapp.repository;

import com.yourcompany.mailapp.entity.Attachment;
import com.yourcompany.mailapp.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    List<Attachment> findByMail(Mail mail);
    
    List<Attachment> findByMailId(Long mailId);
}
