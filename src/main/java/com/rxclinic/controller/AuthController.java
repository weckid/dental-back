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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "true")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String JWT_COOKIE_NAME = "jwt_token";
    private static final int COOKIE_MAX_AGE = 86400;
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthService authService, JwtUtils jwtUtils, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword()); // Пароль будет захеширован в AuthService

            User registeredUser = authService.registerUser(user);
            String token = jwtUtils.generateToken(registeredUser);
            return ResponseEntity.ok(new AuthResponse(token, "Registration successful", registeredUser.getUsername()));
        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(null, "Registration failed: " + e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            User user = authService.authenticateUser(request.getEmail(), request.getPassword());
            String token = jwtUtils.generateToken(user);
            setJwtCookie(response, token);
            return ResponseEntity.ok(new AuthResponse(token, "Login successful", user.getUsername()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, e.getMessage(), null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ResponseEntity.ok(new AuthResponse(null, "Logout successful", null));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // Установите true для HTTPS в продакшене
                .path("/")
                .maxAge(COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("Cookie set: jwt_token={}", token);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JWT_COOKIE_NAME, null)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Not authenticated", null));
        }
        String token = authHeader.substring(7);
        if (!jwtUtils.validateTokenSignature(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Invalid token", null));
        }
        String username = jwtUtils.getUsernameFromToken(token);
        User user = authService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(new AuthResponse(
                null, "Profile data", user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getPhone(), user.getPhotoUrl()
        ));
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @CookieValue(name = "jwt_token", required = false) String token,
            @RequestPart(required = false) String login,
            @RequestPart(required = false) String email,
            @RequestPart(required = false) String firstName,
            @RequestPart(required = false) String lastName,
            @RequestPart(required = false) String phone,
            @RequestPart(required = false) MultipartFile photo,
            @RequestPart(required = false) String oldPassword,
            @RequestPart(required = false) String newPassword
    ) {
        if (token == null || !jwtUtils.validateTokenSignature(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, "Not authenticated", null));
        }
        String username = jwtUtils.getUsernameFromToken(token);
        User user = authService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Обновляем только переданные поля
        if (login != null && !login.isBlank()) {
            user.setUsername(login);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (photo != null && !photo.isEmpty()) {
            try {
                String photoUrl = savePhoto(photo);
                user.setPhotoUrl(photoUrl);
            } catch (IOException e) {
                log.error("Failed to save photo: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new AuthResponse(null, "Ошибка при загрузке фото: " + e.getMessage(), null));
            }
        }

        // Обновление пароля
        if (newPassword != null && !newPassword.isBlank()) {
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse(null, "Old password is incorrect or missing", null));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        authService.saveUser(user);
        return ResponseEntity.ok(new AuthResponse(
                null, "Profile updated", user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getPhone(), user.getPhotoUrl()
        ));
    }

    private String savePhoto(MultipartFile photo) throws IOException {
        // Используем корень проекта для сохранения файлов
        String uploadDirPath = System.getProperty("user.dir") + File.separator + "uploads";
        File uploadDir = new File(uploadDirPath);

        // Создаём директорию, если её нет
        if (!uploadDir.exists()) {
            log.info("Creating upload directory: {}", uploadDir.getAbsolutePath());
            if (!uploadDir.mkdirs()) {
                throw new IOException("Failed to create upload directory: " + uploadDir.getAbsolutePath());
            }
        }

        // Проверяем права на запись
        if (!uploadDir.canWrite()) {
            log.error("Cannot write to directory: {}", uploadDir.getAbsolutePath());
            throw new IOException("Directory is not writable: " + uploadDir.getAbsolutePath());
        }

        // Генерируем уникальное имя файла
        String fileName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();
        File destination = new File(uploadDir, fileName);

        log.info("Saving photo to: {}", destination.getAbsolutePath());

        // Сохраняем файл
        try {
            Files.copy(photo.getInputStream(), destination.toPath());
        } catch (IOException e) {
            log.error("Error saving photo to {}: {}", destination.getAbsolutePath(), e.getMessage(), e);
            throw e;
        }

        // Возвращаем относительный URL
        return "/uploads/" + fileName;
    }
}