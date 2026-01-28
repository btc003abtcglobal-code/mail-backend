package com.yourcompany.mailapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImapConfig {
    
    @Value("${imap.host}")
    private String imapHost;
    
    @Value("${imap.port}")
    private Integer imapPort;
    
    @Value("${imap.protocol}")
    private String imapProtocol;
    
    @Value("${imap.sync-interval}")
    private Long syncInterval;
    
    public String getImapHost() {
        return imapHost;
    }
    
    public Integer getImapPort() {
        return imapPort;
    }
    
    public String getImapProtocol() {
        return imapProtocol;
    }
    
    public Long getSyncInterval() {
        return syncInterval;
    }
}

