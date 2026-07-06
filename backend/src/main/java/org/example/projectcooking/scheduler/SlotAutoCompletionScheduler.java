package org.example.projectcooking.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Автозавершение слота и его CONFIRMED-броней в COMPLETED по наступлении startAt + 180 мин
 * (D-09, §5.1, UC-10). NO_SHOW и отменённые брони сохраняют свой статус.
 */
@Component
@RequiredArgsConstructor
public class SlotAutoCompletionScheduler {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    @Scheduled(
            fixedDelayString = "${projectcooking.scheduling.fixed-delay:PT1M}",
            initialDelayString = "${projectcooking.scheduling.initial-delay:PT1M}")
    @Transactional
    public void completeDueSlots() {
        Instant latestStartAt = Instant.now().minus(Slot.FIXED_DURATION_MINUTES, ChronoUnit.MINUTES);
        for (Slot slot : slotRepository.findByStatusAndStartAtLessThanEqualOrderByStartAtAsc(
                SlotStatus.SCHEDULED, latestStartAt)) {
            slot.setStatus(SlotStatus.COMPLETED);
            for (Booking booking : bookingRepository.findBySlot_IdAndStatus(slot.getId(), BookingStatus.CONFIRMED)) {
                booking.setStatus(BookingStatus.COMPLETED);
            }
        }
    }
}
