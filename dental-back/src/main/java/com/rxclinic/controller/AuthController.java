package com.rxclinic.controller;

import com.rxclinic.DTO.LoginRequest;
import com.rxclinic.model.User;
import com.rxclinic.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = authService.registerUser(user);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка регистрации: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> user = authService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(401).body("Неверный логин или пароль");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка авторизации: " + e.getMessage());
        }
    }
}