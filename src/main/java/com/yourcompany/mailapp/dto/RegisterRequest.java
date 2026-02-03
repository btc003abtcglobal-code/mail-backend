package com.yourcompany.mailapp.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String mailName;
    private String password;
}
