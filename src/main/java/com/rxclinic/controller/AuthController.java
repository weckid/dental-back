package com.rxclinic.controller;

import com.rxclinic.DTO.AuthResponse;
import com.rxclinic.DTO.LoginRequest;
import com.rxclinic.DTO.RegisterRequest;
import com.rxclinic.model.User;
import com.rxclinic.service.AuthService;
import com.rxclinic.util.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"},
        allowCredentials = "true")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String JWT_COOKIE_NAME = "jwt_token";
    private static final int COOKIE_MAX_AGE = 86400;
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
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());

            User registeredUser = authService.registerUser(user);

            try {
                String token = jwtUtils.generateToken(registeredUser);
                return ResponseEntity.ok(new AuthResponse(token, "Registration successful", registeredUser.getUsername()));
            } catch (Exception e) {
                log.error("JWT generation failed for user {}: {}", registeredUser.getUsername(), e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new AuthResponse(null, "Registration partially successful but token generation failed", registeredUser.getUsername()));
            }
        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, "Registration failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        try {
            User user = authService.authenticateUser(request.getEmail(), request.getPassword());
            String token = jwtUtils.generateToken(user);

            // Создаем HTTP-only куку
            ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(false) // true в production
                    .path("/")
                    .maxAge(COOKIE_MAX_AGE)
                    .sameSite("Lax")
                    .build();

            // Возвращаем ответ с кукой в заголовке
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AuthResponse(
                            null, // Токен теперь только в куках
                            "Login successful",
                            user.getUsername()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, e.getMessage(), null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.debug("Test endpoint accessed");
        return ResponseEntity.ok("{\"status\":\"API работает!\"}");
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ResponseEntity.ok(new AuthResponse(null, "Logout successful", null));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(@CookieValue(name = "jwt_token", required = false) String token) {
        if (token == null || !jwtUtils.validateTokenSignature(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwtUtils.getUsernameFromToken(token);
        return ResponseEntity.ok(new AuthResponse(null, "Authenticated", username));
    }
    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request) {
        return ResponseEntity.ok().build();
    }

}