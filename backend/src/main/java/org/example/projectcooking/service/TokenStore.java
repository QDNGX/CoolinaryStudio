package org.example.projectcooking.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.projectcooking.domain.enums.UserRole;
import org.springframework.stereotype.Component;

/**
 * In-memory хранилище непрозрачных (opaque) bearer-токенов (§7, механизм — на усмотрение
 * разработки). Токен выдаётся при verify, снимается при logout; проверяется по каждому запросу.
 * TODO: при необходимости персистентности/масштабирования — вынести в БД или JWT.
 */
@Component
public class TokenStore {

    /** Данные сессии по токену. */
    public record Session(UUID userId, UserRole role) {
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(UUID userId, UserRole role) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessions.put(token, new Session(userId, role));
        return token;
    }

    public Optional<Session> resolve(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }
}
