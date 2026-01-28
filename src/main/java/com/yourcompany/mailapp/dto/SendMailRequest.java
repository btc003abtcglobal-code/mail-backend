package com.yourcompany.mailapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequest {
    
    @NotNull(message = "Mail account ID is required")
    private Long mailAccountId;
    
    @NotBlank(message = "At least one recipient is required")
    private String to;
    
    private String cc;
    
    private String bcc;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String bodyText;
    
    private String bodyHtml;
    
    private List<Long> attachmentIds;
    
    private String inReplyTo;
    
    private String references;
}

