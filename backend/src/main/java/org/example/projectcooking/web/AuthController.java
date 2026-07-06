package org.example.projectcooking.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.auth.AuthCodeRequest;
import org.example.projectcooking.dto.auth.AuthCodeVerify;
import org.example.projectcooking.dto.auth.AuthResult;
import org.example.projectcooking.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Passwordless-вход (UC-05, ФТ-13, ФТ-21). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    /** 202 всегда — ответ не раскрывает существование аккаунта (Р-01). */
    @PostMapping("/code/request")
    public ResponseEntity<Void> requestCode(@Valid @RequestBody AuthCodeRequest request) {
        authService.requestCode(request.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/code/verify")
    public AuthResult verify(@Valid @RequestBody AuthCodeVerify request) {
        return authService.verify(request.getEmail(), request.getCode());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            authService.logout(authHeader.substring(BEARER_PREFIX.length()).trim());
        }
        return ResponseEntity.noContent().build();
    }
}
