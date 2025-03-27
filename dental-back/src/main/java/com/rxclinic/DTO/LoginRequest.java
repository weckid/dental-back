package com.rxclinic.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email
    private String email; // Измените username на email

    @NotBlank(message = "Password is required")
    private String password;

}