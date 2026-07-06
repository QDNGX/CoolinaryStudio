package org.example.projectcooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Расширение User для role=CLIENT (1:1, общий PK) — tz-11 §2.2.
 * lateCancelCount / blockedUntil — производные (D-02, D-11): меняет только система.
 */
@Entity
@Table(name = "client_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientProfile {

    @Id
    private UUID userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** Заметка об аллергии (ФТ-18); ≤ 500 (Р-16); читает шеф в списке участников. */
    @Column(length = 500)
    private String allergyNote;

    /** Счётчик поздних отмен/неявок (D-02); только система. */
    @Builder.Default
    @Column(nullable = false)
    private int lateCancelCount = 0;

    /** Дата разблокировки при активном блоке (D-02); только система. */
    private Instant blockedUntil;

    /** Порог нарушений для блокировки (D-02, ФТ-09). */
    public static final int BLOCK_THRESHOLD = 3;

    /** Длительность блокировки (D-02, ФТ-09). */
    public static final Duration BLOCK_DURATION = Duration.ofDays(7);

    /** Активна ли блокировка на момент {@code now} (D-02). */
    public boolean isBlocked(Instant now) {
        return blockedUntil != null && now.isBefore(blockedUntil);
    }

    /**
     * Ленивый сброс: по наступлении blockedUntil блок снимается и счётчик обнуляется (D-02, КП-5).
     * Вызывается при проверке блокировки (создание брони, GET /me).
     */
    public void resetBlockIfExpired(Instant now) {
        if (blockedUntil != null && !now.isBefore(blockedUntil)) {
            blockedUntil = null;
            lateCancelCount = 0;
        }
    }

    /** Поздняя отмена или неявка: +1 к счётчику, при достижении порога — блок на 7 дней (D-02). */
    public void registerViolation(Instant now) {
        lateCancelCount++;
        if (lateCancelCount >= BLOCK_THRESHOLD) {
            blockedUntil = now.plus(BLOCK_DURATION);
        }
    }

    /** Снятие ошибочной неявки админом: −1 к счётчику, снятие блока если условие отпало (D-11). */
    public void revokeViolation() {
        if (lateCancelCount > 0) {
            lateCancelCount--;
        }
        if (lateCancelCount < BLOCK_THRESHOLD) {
            blockedUntil = null;
        }
    }
}
