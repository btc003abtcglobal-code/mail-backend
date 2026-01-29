package com.yourcompany.mailapp.dto;

import com.yourcompany.mailapp.entity.User;

public class MailUserRequest {
    private String username;
    private String password;
    private User.AccountType accountType;
    private String firstName;
    private String lastName;

    public MailUserRequest() {
    }

    public MailUserRequest(String username, String password, User.AccountType accountType) {
        this.username = username;
        this.password = password;
        this.accountType = accountType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User.AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(User.AccountType accountType) {
        this.accountType = accountType;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
