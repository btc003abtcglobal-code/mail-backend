package com.yourcompany.mailapp.scheduler;

import com.yourcompany.mailapp.service.mail.MailSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImapSyncJob {
    
    private final MailSyncService mailSyncService;
    
    /**
     * Runs every 5 minutes by default (configurable via imap.sync-interval)
     */
    @Scheduled(fixedDelayString = "${imap.sync-interval:300000}", initialDelay = 60000)
    public void syncAllMailAccounts() {
        log.debug("IMAP sync job triggered");
        mailSyncService.syncAllAccounts();
    }
}

