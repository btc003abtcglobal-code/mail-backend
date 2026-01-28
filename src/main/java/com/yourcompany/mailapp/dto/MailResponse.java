package com.yourcompany.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailResponse {
    
    private Long id;
    private String uid;
    private String messageId;
    private String fromAddress;
    private String fromName;
    private String toAddresses;
    private String ccAddresses;
    private String subject;
    private String bodyText;
    private String bodyHtml;
    private LocalDateTime sentDate;
    private LocalDateTime receivedDate;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean hasAttachments;
    private Long sizeBytes;
    private String folderName;
    private List<AttachmentResponse> attachments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentResponse {
        private Long id;
        private String filename;
        private String contentType;
        private Long sizeBytes;
        private Boolean isInline;
    }
}