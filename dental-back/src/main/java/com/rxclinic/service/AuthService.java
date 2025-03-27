package com.rxclinic.service;

import com.rxclinic.model.User;
import com.rxclinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        // Проверка на существующего пользователя
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword())); // Хешируем здесь
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticateUser(String login, String password) {
        log.info("Authentication attempt for: {}", login);

        // Ищем по username ИЛИ email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(login, login);

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed - user not found: {}", login);
            throw new UsernameNotFoundException("Invalid credentials"); // Общее сообщение для безопасности
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed - invalid password for user: {}", login);
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("Authentication successful for user: {}", user.getUsername());
        return user;
    }
}