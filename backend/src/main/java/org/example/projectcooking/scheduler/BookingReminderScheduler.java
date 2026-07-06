package org.example.projectcooking.scheduler;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email-напоминания за 24ч и 2ч до slot.startAt по CONFIRMED-броням (EML-03/EML-04).
 * Факт отправки фиксируется на брони, поэтому каждый тип письма отправляется максимум один раз.
 */
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private static final Duration REMINDER_24H = Duration.ofHours(24);
    private static final Duration REMINDER_2H = Duration.ofHours(2);
    private static final Duration LOOKBACK_WINDOW = Duration.ofMinutes(5);

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Scheduled(
            fixedDelayString = "${projectcooking.scheduling.fixed-delay:PT1M}",
            initialDelayString = "${projectcooking.scheduling.initial-delay:PT1M}")
    @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        send24h(now);
        send2h(now);
    }

    private void send24h(Instant now) {
        Instant windowEnd = now.plus(REMINDER_24H);
        Instant windowStart = windowEnd.minus(LOOKBACK_WINDOW);
        for (Booking booking : bookingRepository.findDue24hReminders(
                BookingStatus.CONFIRMED, SlotStatus.SCHEDULED, now, windowStart, windowEnd)) {
            booking.setReminder24hSentAt(now);
            emailService.sendReminder24h(
                    booking.getClient().getUser().getEmail(),
                    booking.getSlot().getProgram().getTitle(),
                    booking.getSlot().getStartAt());
        }
    }

    private void send2h(Instant now) {
        Instant windowEnd = now.plus(REMINDER_2H);
        Instant windowStart = windowEnd.minus(LOOKBACK_WINDOW);
        for (Booking booking : bookingRepository.findDue2hReminders(
                BookingStatus.CONFIRMED, SlotStatus.SCHEDULED, now, windowStart, windowEnd)) {
            booking.setReminder2hSentAt(now);
            emailService.sendReminder2h(
                    booking.getClient().getUser().getEmail(),
                    booking.getSlot().getProgram().getTitle(),
                    booking.getSlot().getStartAt());
        }
    }
}
