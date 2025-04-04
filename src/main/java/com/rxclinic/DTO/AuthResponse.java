package com.rxclinic.DTO;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String message;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String photoUrl;

    public AuthResponse(String token, String message, String username) {
        this.token = token;
        this.message = message;
        this.username = username;
    }

    public AuthResponse(
            String token,
            String message,
            String username,
            String email,
            String firstName,
            String lastName,
            String phone,
            String photoUrl
    ) {
        this.token = token;
        this.message = message;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.photoUrl = photoUrl;
    }
}