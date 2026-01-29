package com.yourcompany.mailapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MailResponse {
    private String from;
    private String subject;
    private String body;
}