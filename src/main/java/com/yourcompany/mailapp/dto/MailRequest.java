package com.yourcompany.mailapp.dto;

import lombok.Data;

@Data
public class MailRequest {
    private String to;
    private String subject;
    private String message;
}
