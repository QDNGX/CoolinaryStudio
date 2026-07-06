package org.example.projectcooking.dto.user;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Профиль клиента в составе /me (tz-11 §2.2). Дисциплинарные поля — только для чтения (D-02). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfileResponse {

    private String allergyNote;

    /** «поздних отмен N из 3» (Р-06). */
    private int lateCancelCount;

    private Instant blockedUntil;
}
