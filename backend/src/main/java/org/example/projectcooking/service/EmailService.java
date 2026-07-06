package org.example.projectcooking.service;

/**
 * Отправка email-уведомлений (tz-11 §6). Все методы — best-effort (D-07/НФТ-03): реализация
 * не должна бросать исключения в вызывающую транзакцию. Канал — только email (D-07).
 */
public interface EmailService {

    /** EML-01 — одноразовый код входа. */
    void sendLoginCode(String email, String code);

    /** EML-02 — отмена класса студией с причиной (ФТ-15). */
    void sendSlotCancellation(String email, String programTitle, String reason);

    /** EML-03 — напоминание за 24 часа до класса. */
    void sendReminder24h(String email, String programTitle, java.time.Instant startAt);

    /** EML-04 — напоминание за 2 часа до класса. */
    void sendReminder2h(String email, String programTitle, java.time.Instant startAt);
}
