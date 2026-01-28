package com.yourcompany.mailapp.service.mail;

import com.yourcompany.mailapp.entity.MailAccount;
import com.yourcompany.mailapp.repository.MailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailSyncService {

    private final MailAccountRepository mailAccountRepository;

    public void syncAllAccounts() {
        log.info("Starting sync for all mail accounts...");
        List<MailAccount> accounts = mailAccountRepository.findAll();
        for (MailAccount account : accounts) {
            syncAccount(account);
        }
        log.info("Completed sync for {} accounts", accounts.size());
    }

    public void syncAccount(Long accountId) {
        MailAccount account = mailAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Mail account not found with ID: " + accountId));
        syncAccount(account);
    }

    private void syncAccount(MailAccount account) {
        log.info("Syncing account: {}", account.getId());
        // TODO: Implement actual IMAP sync logic here
    }
}
