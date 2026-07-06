package org.example.projectcooking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory TTL-хранилище кодов подтверждения входа (tz-11 §2.9, НФТ-05 — не доменная сущность).
 * Обеспечивает проверки НФТ-04/Р-09: TTL 10 минут, ≤ 5 попыток, повтор не чаще 1 раза в 60 сек.
 * Истёкшие записи вычищаются лениво при обращении (отдельный планировщик не нужен).
 */
@Component
public class AuthCodeStore {

    public static final Duration CODE_TTL = Duration.ofMinutes(10);
    public static final int MAX_ATTEMPTS = 5;
    public static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);

    /** Итог проверки кода. */
    public enum VerifyStatus {
        OK,
        INVALID_CODE,
        CODE_EXPIRED,
        ATTEMPTS_EXCEEDED
    }

    private static final class Entry {
        String code;
        Instant issuedAt;
        Instant lastSentAt;
        int attempts;
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /** Сколько секунд ещё нельзя переотправлять код (0 — можно). */
    public synchronized long resendWaitSeconds(String email, Instant now) {
        Entry e = store.get(key(email));
        if (e == null) {
            return 0;
        }
        long elapsed = Duration.between(e.lastSentAt, now).getSeconds();
        return Math.max(0, RESEND_INTERVAL.getSeconds() - elapsed);
    }

    /** Выдать новый код (сбрасывает счётчик попыток). Заменяет предыдущий — валиден только последний. */
    public synchronized void issue(String email, String code, Instant now) {
        Entry e = new Entry();
        e.code = code;
        e.issuedAt = now;
        e.lastSentAt = now;
        e.attempts = 0;
        store.put(key(email), e);
    }

    /** Проверить код: OK удаляет запись; неверный код инкрементирует счётчик (НФТ-04). */
    public synchronized VerifyStatus verify(String email, String code, Instant now) {
        Entry e = store.get(key(email));
        if (e == null || Duration.between(e.issuedAt, now).compareTo(CODE_TTL) > 0) {
            store.remove(key(email));
            return VerifyStatus.CODE_EXPIRED;
        }
        if (e.attempts >= MAX_ATTEMPTS) {
            return VerifyStatus.ATTEMPTS_EXCEEDED;
        }
        if (!e.code.equals(code)) {
            e.attempts++;
            return e.attempts >= MAX_ATTEMPTS ? VerifyStatus.ATTEMPTS_EXCEEDED : VerifyStatus.INVALID_CODE;
        }
        store.remove(key(email));
        return VerifyStatus.OK;
    }

    private String key(String email) {
        return email.toLowerCase();
    }
}
