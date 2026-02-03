package com.yourcompany.mailapp.dto;

import lombok.Data;

@Data
public class RegisterMailRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
