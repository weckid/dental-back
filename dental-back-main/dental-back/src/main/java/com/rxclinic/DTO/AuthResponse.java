package com.rxclinic.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String message;
    private String username;

    // Минимальный конструктор
    public AuthResponse(String token) {
        this.token = token;
        this.message = "Success";
        this.username = null;
    }
}