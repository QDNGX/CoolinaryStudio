package org.example.projectcooking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Заглушка доставки email: пишет письма в лог (SMTP в MVP не подключён; отчёта о недоставке
 * нет — Р-16 п.5). Так же в лог печатается код входа — им и проверяется passwordless-вход.
 * Все методы best-effort: исключения проглатываются, чтобы не влиять на транзакции (НФТ-03).
 */
@Slf4j
@Service
public class LoggingEmailService implements EmailService {

    @Override
    public void sendLoginCode(String email, String code) {
        safe(() -> log.warn("""

                ============================================================
                LOGIN CODE / КОД ВХОДА
                Email: {}
                Code:  {}
                Valid: 10 minutes
                ============================================================
                """, email, code));
    }

    @Override
    public void sendSlotCancellation(String email, String programTitle, String reason) {
        safe(() -> log.warn("[EML-02][SLOT CANCELLED] Клиенту {}: класс «{}» отменён студией. Причина: {}",
                email, programTitle, reason));
    }

    @Override
    public void sendReminder24h(String email, String programTitle, java.time.Instant startAt) {
        safe(() -> log.info("[EML-03] Клиенту {}: напоминание за 24 часа — класс «{}» начнётся {}",
                email, programTitle, startAt));
    }

    @Override
    public void sendReminder2h(String email, String programTitle, java.time.Instant startAt) {
        safe(() -> log.info("[EML-04] Клиенту {}: напоминание за 2 часа — класс «{}» начнётся {}",
                email, programTitle, startAt));
    }

    private void safe(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Сбой отправки email (best-effort, игнорируется): {}", ex.getMessage());
        }
    }
}
