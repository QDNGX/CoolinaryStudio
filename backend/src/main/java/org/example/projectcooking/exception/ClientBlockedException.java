package org.example.projectcooking.exception;

import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Активная блокировка клиента (D-02): новые брони запрещены до {@link #blockedUntil}.
 * Отдаётся как 403 в форме {@code BlockedError} (Error + blockedUntil).
 */
@Getter
public class ClientBlockedException extends ApiException {

    private final Instant blockedUntil;

    public ClientBlockedException(Instant blockedUntil) {
        super(HttpStatus.FORBIDDEN, "CLIENT_BLOCKED",
                "Из-за поздних отмен новые брони недоступны до " + blockedUntil,
                Map.of("blockedUntil", blockedUntil));
        this.blockedUntil = blockedUntil;
    }
}
