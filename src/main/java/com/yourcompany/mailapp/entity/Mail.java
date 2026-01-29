package com.yourcompany.mailapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mails", indexes = {
        @Index(name = "idx_mail_message_id", columnList = "message_id"),
        @Index(name = "idx_mail_account", columnList = "mail_account_id"),
        @Index(name = "idx_mail_folder", columnList = "folder_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uid;

    @Column(name = "message_id", unique = true)
    private String messageId;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "from_name")
    private String fromName;

    @Column(name = "to_addresses", columnDefinition = "TEXT")
    private String toAddresses;

    @Column(name = "cc_addresses", columnDefinition = "TEXT")
    private String ccAddresses;

    @Column(name = "bcc_addresses", columnDefinition = "TEXT")
    private String bccAddresses;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Lob
    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;

    @Lob
    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_starred")
    private Boolean isStarred = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_account_id")
    private MailAccount mailAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    @Column(name = "message_references", columnDefinition = "TEXT")
    private String references;

    @Column(name = "has_attachments")
    private Boolean hasAttachments = false;

    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
