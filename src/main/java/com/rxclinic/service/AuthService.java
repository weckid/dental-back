package com.rxclinic.service;

import com.rxclinic.model.Role;
import com.rxclinic.model.User;
import com.rxclinic.repository.RoleRepository;
import com.rxclinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user) {
        log.info("Attempting to register user: {}", user.getEmail());
        if (user.getEmail() == null || user.getUsername() == null) {
            throw new IllegalArgumentException("Email and username cannot be null");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' already in use");
        }

        // Назначаем роль USER по умолчанию
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
        user.setRoles(Collections.singleton(userRole));
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Хешируем здесь один раз
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticateUser(String login, String password) {
        log.info("Authentication attempt for: {}", login);

        Optional<User> userOpt = userRepository.findByUsernameOrEmail(login, login);

        if (userOpt.isEmpty()) {
            log.warn("Authentication failed - user not found: {}", login);
            throw new UsernameNotFoundException("Invalid credentials");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed - invalid password for user: {}", login);
            throw new IllegalArgumentException("Invalid credentials");
        }

        log.info("Authentication successful for user: {}", user.getUsername());
        return user;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}