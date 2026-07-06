package org.example.projectcooking.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Повторная отправка кода раньше 60 секунд (НФТ-04) → 429 с заголовком {@code Retry-After}.
 */
@Getter
public class RateLimitException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Повторная отправка кода будет доступна позже",
                Map.of("retryAfterSeconds", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
