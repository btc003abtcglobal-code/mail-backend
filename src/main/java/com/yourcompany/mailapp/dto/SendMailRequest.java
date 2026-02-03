package com.yourcompany.mailapp.dto;

import lombok.Data;

@Data
public class SendMailRequest {
    private String to;
    private String subject;
    private String body;
    private String password; // User's mail password
}
