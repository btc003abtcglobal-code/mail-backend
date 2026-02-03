package com.yourcompany.mailapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class MailAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String emailAddress;
    private String displayName;
    private String vpsUsername;
    private String vpsPassword;
    private String smtpHost;
    private Integer smtpPort;
    private String imapHost;
    private Integer imapPort;
    private String password;
    private Boolean smtpUseTls;
    private Boolean imapUseSsl;
    private Boolean isDefault;
    private Boolean active;
    private LocalDateTime lastSync;
}
