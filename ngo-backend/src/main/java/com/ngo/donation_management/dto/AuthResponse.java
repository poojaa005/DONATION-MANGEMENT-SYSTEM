package com.ngo.donation_management.dto;

// dto/AuthResponse.java

public class AuthResponse {
    private String token;
    private Integer userId;
    private String email;
    private String role;
    private String name;
    private Integer ngoId;
    private String ngoName;

    public AuthResponse() {
    }

    public AuthResponse(String token, Integer userId, String email,
                        String role, String name,
                        Integer ngoId, String ngoName) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.name = name;
        this.ngoId = ngoId;
        this.ngoName = ngoName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNgoId() {
        return ngoId;
    }

    public void setNgoId(Integer ngoId) {
        this.ngoId = ngoId;
    }

    public String getNgoName() {
        return ngoName;
    }

    public void setNgoName(String ngoName) {
        this.ngoName = ngoName;
    }
}
