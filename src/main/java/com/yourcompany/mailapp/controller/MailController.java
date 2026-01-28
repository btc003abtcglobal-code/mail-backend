package com.yourcompany.mailapp.controller;

import com.yourcompany.mailapp.dto.SendMailRequest;
import com.yourcompany.mailapp.entity.Mail;
import com.yourcompany.mailapp.service.mail.AttachmentService;
import com.yourcompany.mailapp.service.mail.MailReceiveService;
import com.yourcompany.mailapp.service.mail.MailSendService;
import com.yourcompany.mailapp.service.mail.MailSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for mail operations
 * Handles sending, receiving, searching, and managing emails
 */
@Slf4j
@RestController
@RequestMapping("/api/mails")
@RequiredArgsConstructor
public class MailController {

    private final MailSendService mailSendService;
    private final MailReceiveService mailReceiveService;
    private final MailSyncService mailSyncService;
    private final AttachmentService attachmentService;

    

    // ==================== Send Mail Operations ====================

    /**
     * Send a new email
     * 
     * @param request     send mail request with recipients, subject, body
     * @param userDetails authenticated user
     * @return response with mail ID
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMail(
            @Valid @RequestBody SendMailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Send mail request from user: {} to: {}",
                    userDetails.getUsername(), request.getTo());

            Mail mail = mailSendService.sendMail(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mail sent successfully");
            response.put("mailId", mail.getId());
            response.put("messageId", mail.getMessageId());
            response.put("sentDate", mail.getSentDate());

            log.info("Mail sent successfully. Mail ID: {}", mail.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending mail", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Reply to an email
     * 
     * @param mailId      original mail ID
     * @param request     reply content
     * @param userDetails authenticated user
     * @return response with reply mail ID
     */
    @PostMapping("/{mailId}/reply")
    public ResponseEntity<?> replyMail(
            @PathVariable Long mailId,
            @Valid @RequestBody SendMailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Reply to mail {} from user: {}", mailId, userDetails.getUsername());

            Mail mail = mailSendService.replyMail(mailId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reply sent successfully");
            response.put("mailId", mail.getId());
            response.put("inReplyTo", mail.getInReplyTo());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error replying to mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Reply to all recipients
     * 
     * @param mailId      original mail ID
     * @param request     reply content
     * @param userDetails authenticated user
     * @return response with reply mail ID
     */
    @PostMapping("/{mailId}/reply-all")
    public ResponseEntity<?> replyAllMail(
            @PathVariable Long mailId,
            @Valid @RequestBody SendMailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Reply-all to mail {} from user: {}", mailId, userDetails.getUsername());

            // Get original mail to include all recipients
            Mail originalMail = mailReceiveService.getMailById(mailId);

            // Add original To and CC to the reply
            StringBuilder recipients = new StringBuilder();
            if (originalMail.getToAddresses() != null) {
                recipients.append(originalMail.getToAddresses());
            }
            if (originalMail.getCcAddresses() != null && !originalMail.getCcAddresses().isEmpty()) {
                if (recipients.length() > 0)
                    recipients.append(",");
                recipients.append(originalMail.getCcAddresses());
            }

            if (recipients.length() > 0) {
                request.setCc(recipients.toString());
            }

            Mail mail = mailSendService.replyMail(mailId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reply sent to all recipients");
            response.put("mailId", mail.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in reply-all to mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Forward an email
     * 
     * @param mailId      mail to forward
     * @param request     forward recipients and message
     * @param userDetails authenticated user
     * @return response with forwarded mail ID
     */
    @PostMapping("/{mailId}/forward")
    public ResponseEntity<?> forwardMail(
            @PathVariable Long mailId,
            @Valid @RequestBody SendMailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Forward mail {} from user: {}", mailId, userDetails.getUsername());

            Mail mail = mailSendService.forwardMail(mailId, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mail forwarded successfully");
            response.put("mailId", mail.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error forwarding mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Retrieve Mail Operations ====================

    /**
     * Get single mail by ID
     * 
     * @param mailId      mail ID
     * @param userDetails authenticated user
     * @return mail details
     */
    @GetMapping("/{mailId}")
    public ResponseEntity<?> getMail(
            @PathVariable Long mailId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Get mail {} for user: {}", mailId, userDetails.getUsername());

            Mail mail = mailReceiveService.getMailById(mailId);

            return ResponseEntity.ok(mail);

        } catch (Exception e) {
            log.error("Error fetching mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get mails in a folder (paginated)
     * 
     * @param folderId    folder ID
     * @param page        page number (default: 0)
     * @param size        page size (default: 20)
     * @param sortBy      sort field (default: receivedDate)
     * @param sortDir     sort direction (default: desc)
     * @param userDetails authenticated user
     * @return page of mails
     */
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<?> getMailsByFolder(
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.debug("Get mails for folder {} - page: {}, size: {}", folderId, page, size);

            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Mail> mails = mailReceiveService.getMailsByFolder(folderId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("mails", mails.getContent());
            response.put("currentPage", mails.getNumber());
            response.put("totalItems", mails.getTotalElements());
            response.put("totalPages", mails.getTotalPages());
            response.put("hasNext", mails.hasNext());
            response.put("hasPrevious", mails.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching mails for folder {}", folderId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get unread mails count for folder
     * 
     * @param folderId folder ID
     * @return unread count
     */
    @GetMapping("/folder/{folderId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long folderId) {
        try {
            Long count = mailReceiveService.getUnreadCount(folderId);

            Map<String, Object> response = new HashMap<>();
            response.put("folderId", folderId);
            response.put("unreadCount", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting unread count for folder {}", folderId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Search Operations ====================

    /**
     * Search mails by keyword
     * 
     * @param mailAccountId mail account ID
     * @param keyword       search keyword
     * @param page          page number
     * @param size          page size
     * @param userDetails   authenticated user
     * @return page of matching mails
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMails(
            @RequestParam Long mailAccountId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Search mails for keyword: '{}' in account: {}", keyword, mailAccountId);

            Pageable pageable = PageRequest.of(page, size, Sort.by("receivedDate").descending());
            Page<Mail> mails = mailReceiveService.searchMails(mailAccountId, keyword, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("mails", mails.getContent());
            response.put("currentPage", mails.getNumber());
            response.put("totalItems", mails.getTotalElements());
            response.put("totalPages", mails.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching mails", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Advanced search with multiple filters
     * 
     * @param filters search filters
     * @return page of matching mails
     */
    @PostMapping("/search/advanced")
    public ResponseEntity<?> advancedSearch(@RequestBody Map<String, Object> filters) {
        try {
            log.info("Advanced search with filters: {}", filters);

            // Extract filters
            Long mailAccountId = ((Number) filters.get("mailAccountId")).longValue();
            String keyword = (String) filters.get("keyword");
            String fromAddress = (String) filters.get("from");
            Boolean hasAttachments = (Boolean) filters.get("hasAttachments");
            Boolean isRead = (Boolean) filters.get("isRead");

            int page = filters.containsKey("page") ? ((Number) filters.get("page")).intValue() : 0;
            int size = filters.containsKey("size") ? ((Number) filters.get("size")).intValue() : 20;

            Pageable pageable = PageRequest.of(page, size, Sort.by("receivedDate").descending());

            // Perform search
            Page<Mail> mails = mailReceiveService.advancedSearch(
                    mailAccountId, keyword, fromAddress, hasAttachments, isRead, pageable);

            return ResponseEntity.ok(mails);

        } catch (Exception e) {
            log.error("Error in advanced search", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Mail Status Operations ====================

    /**
     * Mark mail as read/unread
     * 
     * @param mailId      mail ID
     * @param read        read status
     * @param userDetails authenticated user
     * @return success message
     */
    @PutMapping("/{mailId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long mailId,
            @RequestParam boolean read,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Mark mail {} as {} by user: {}",
                    mailId, read ? "read" : "unread", userDetails.getUsername());

            mailReceiveService.markAsRead(mailId, read);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mail marked as " + (read ? "read" : "unread"));
            response.put("mailId", mailId);
            response.put("isRead", read);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking mail {} as read", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Mark mail as starred/unstarred
     * 
     * @param mailId      mail ID
     * @param starred     starred status
     * @param userDetails authenticated user
     * @return success message
     */
    @PutMapping("/{mailId}/starred")
    public ResponseEntity<?> markAsStarred(
            @PathVariable Long mailId,
            @RequestParam boolean starred,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Mark mail {} as {} by user: {}",
                    mailId, starred ? "starred" : "unstarred", userDetails.getUsername());

            mailReceiveService.markAsStarred(mailId, starred);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mail " + (starred ? "starred" : "unstarred"));
            response.put("mailId", mailId);
            response.put("isStarred", starred);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error marking mail {} as starred", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Mark multiple mails as read
     * 
     * @param request contains list of mail IDs
     * @return success message
     */
    @PutMapping("/bulk/mark-read")
    public ResponseEntity<?> bulkMarkAsRead(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> mailIds = request.get("mailIds");
            log.info("Bulk mark as read for {} mails", mailIds.size());

            for (Long mailId : mailIds) {
                mailReceiveService.markAsRead(mailId, true);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mails marked as read");
            response.put("count", mailIds.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in bulk mark as read", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Delete Operations ====================

    /**
     * Soft delete a mail (move to trash)
     * 
     * @param mailId      mail ID
     * @param userDetails authenticated user
     * @return success message
     */
    @DeleteMapping("/{mailId}")
    public ResponseEntity<?> deleteMail(
            @PathVariable Long mailId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Delete mail {} by user: {}", mailId, userDetails.getUsername());

            mailReceiveService.deleteMail(mailId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mail deleted successfully");
            response.put("mailId", mailId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete multiple mails
     * 
     * @param request contains list of mail IDs
     * @return success message
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<?> bulkDelete(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> mailIds = request.get("mailIds");
            log.info("Bulk delete {} mails", mailIds.size());

            for (Long mailId : mailIds) {
                mailReceiveService.deleteMail(mailId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Mails deleted successfully");
            response.put("count", mailIds.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in bulk delete", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Sync Operations ====================

    /**
     * Manually sync mail account
     * 
     * @param mailAccountId mail account ID
     * @param userDetails   authenticated user
     * @return sync result
     */
    @PostMapping("/sync/{mailAccountId}")
    public ResponseEntity<?> syncAccount(
            @PathVariable Long mailAccountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Manual sync requested for account {} by user: {}",
                    mailAccountId, userDetails.getUsername());

            mailSyncService.syncAccount(mailAccountId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sync completed successfully");
            response.put("mailAccountId", mailAccountId);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing account {}", mailAccountId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Attachment Operations ====================

    /**
     * Upload attachment
     * 
     * @param file        file to upload
     * @param userDetails authenticated user
     * @return attachment info
     */
    @PostMapping("/attachments/upload")
    public ResponseEntity<?> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Upload attachment: {} ({} bytes) by user: {}",
                    file.getOriginalFilename(), file.getSize(), userDetails.getUsername());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File is empty"));
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("File size exceeds 10MB limit"));
            }

            var attachment = attachmentService.uploadAttachment(file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("attachmentId", attachment.getId());
            response.put("filename", attachment.getFilename());
            response.put("size", attachment.getSizeBytes());
            response.put("contentType", attachment.getContentType());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading attachment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Download attachment
     * 
     * @param attachmentId attachment ID
     * @param userDetails  authenticated user
     * @return file resource
     */
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            log.info("Download attachment {} by user: {}", attachmentId, userDetails.getUsername());

            var attachment = attachmentService.getAttachment(attachmentId);
            Resource resource = attachmentService.getAttachmentFile(attachmentId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading attachment {}", attachmentId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get attachments for a mail
     * 
     * @param mailId mail ID
     * @return list of attachments
     */
    @GetMapping("/{mailId}/attachments")
    public ResponseEntity<?> getMailAttachments(@PathVariable Long mailId) {
        try {
            var attachments = attachmentService.getAttachmentsByMailId(mailId);

            Map<String, Object> response = new HashMap<>();
            response.put("mailId", mailId);
            response.put("attachments", attachments);
            response.put("count", attachments.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching attachments for mail {}", mailId, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Create error response
     * 
     * @param message error message
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("Unexpected error in MailController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("An unexpected error occurred"));
    }
}