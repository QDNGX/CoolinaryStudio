package org.example.projectcooking.dto.error;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отказ по активной блокировке клиента (D-02) — {@code BlockedError} = Error + blockedUntil. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedErrorResponse {

    private String code;
    private String message;
    private Map<String, Object> details;
    private Instant blockedUntil;
}
