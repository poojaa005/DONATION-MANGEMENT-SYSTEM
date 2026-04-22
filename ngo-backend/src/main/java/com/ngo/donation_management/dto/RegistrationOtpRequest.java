package com.ngo.donation_management.dto;

public class RegistrationOtpRequest {
    private String email;
    private String name;

    public RegistrationOtpRequest() {
    }

    public RegistrationOtpRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
