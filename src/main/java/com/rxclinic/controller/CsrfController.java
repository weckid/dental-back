package com.rxclinic.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CsrfController {
    @GetMapping("/csrf")
    public CsrfToken getCsrfToken(CsrfToken token) {
        System.out.println("Запрос /api/csrf, токен: " + token.getToken());
        return token;
    }
}