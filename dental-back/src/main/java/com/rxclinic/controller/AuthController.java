package com.rxclinic.controller;

import com.rxclinic.DTO.AuthResponse;
import com.rxclinic.DTO.LoginRequest;
import com.rxclinic.DTO.RegisterRequest;
import com.rxclinic.model.User;
import com.rxclinic.service.AuthService;
import com.rxclinic.util.JwtUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"},
        allowCredentials = "true")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthController(AuthService authService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Attempting to register user: {}", registerRequest.getUsername());

            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());

            User registeredUser = authService.registerUser(user);
            String token = jwtUtils.generateToken(registeredUser);

            log.info("User registered successfully: {}", registeredUser.getUsername());

            return ResponseEntity.ok(
                    new AuthResponse(
                            token,
                            "Registration successful",
                            registeredUser.getUsername()
                    )
            );
        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, "Registration failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());

            User user = authService.authenticateUser(request.getUsername(), request.getPassword());
            String token = jwtUtils.generateToken(user);

            log.info("User logged in successfully: {}", user.getUsername());

            return ResponseEntity.ok(
                    new AuthResponse(
                            token,
                            "Login successful",
                            user.getUsername()
                    )
            );
        } catch (UsernameNotFoundException | IllegalArgumentException e) {
            log.warn("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, e.getMessage(), null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.debug("Test endpoint accessed");
        return ResponseEntity.ok("{\"status\":\"API работает!\"}");
    }
}