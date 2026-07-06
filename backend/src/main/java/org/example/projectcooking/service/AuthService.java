package org.example.projectcooking.service;

import java.security.SecureRandom;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.User;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.auth.AuthResult;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.exception.RateLimitException;
import org.example.projectcooking.mapper.UserMapper;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Passwordless-вход (UC-05, ФТ-13): запрос кода, проверка кода с выдачей bearer-токена, logout.
 * Молчаливое создание клиента при неизвестном email (ФТ-21, D-08 — всегда CLIENT).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final AuthCodeStore codeStore;
    private final TokenStore tokenStore;
    private final EmailService emailService;
    private final UserMapper userMapper;

    private final SecureRandom random = new SecureRandom();

    /**
     * Запрос кода (POST /auth/code/request). Всегда «успех» (202) — не раскрываем существование
     * аккаунта (Р-01/ФТ-21). Повтор чаще 1 раза в 60 сек → RateLimitException → 429 (НФТ-04).
     */
    public void requestCode(String email) {
        Instant now = Instant.now();
        long wait = codeStore.resendWaitSeconds(email, now);
        if (wait > 0) {
            throw new RateLimitException(wait);
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        codeStore.issue(email, code, now);
        emailService.sendLoginCode(email, code);
    }

    /** Проверка кода (POST /auth/code/verify). Успех → bearer-токен + данные пользователя. */
    @Transactional
    public AuthResult verify(String email, String code) {
        Instant now = Instant.now();
        AuthCodeStore.VerifyStatus status = codeStore.verify(email, code, now);
        switch (status) {
            case INVALID_CODE -> throw ApiException.badRequest("INVALID_CODE", "Неверный код");
            case CODE_EXPIRED -> throw ApiException.badRequest("CODE_EXPIRED", "Срок действия кода истёк — запросите новый");
            case ATTEMPTS_EXCEEDED -> throw ApiException.badRequest("ATTEMPTS_EXCEEDED", "Исчерпаны попытки — запросите новый код");
            case OK -> { /* продолжаем */ }
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNewUser = false;
        ClientProfile clientProfile = null;
        ChefProfile chefProfile = null;

        if (user == null) {
            // Неизвестный email — всегда новый CLIENT (ФТ-21, D-08); CHEF/ADMIN заводит только админ.
            user = userRepository.save(User.builder()
                    .role(UserRole.CLIENT)
                    .email(email)
                    .enabled(true)
                    .build());
            clientProfile = clientProfileRepository.save(ClientProfile.builder().user(user).build());
            isNewUser = true;
        } else if (user.getRole() == UserRole.CLIENT) {
            clientProfile = clientProfileRepository.findById(user.getId()).orElse(null);
        } else if (user.getRole() == UserRole.CHEF) {
            chefProfile = chefProfileRepository.findById(user.getId()).orElse(null);
        }

        String token = tokenStore.issue(user.getId(), user.getRole());
        return AuthResult.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .isNewUser(isNewUser)
                .user(userMapper.toCurrentUser(user, clientProfile, chefProfile))
                .build();
    }

    /** Завершение сессии (POST /auth/logout) — инвалидация токена (204). */
    public void logout(String token) {
        if (token != null) {
            tokenStore.invalidate(token);
        }
    }
}
