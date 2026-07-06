package org.example.projectcooking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.booking.ParticipantBookingResponse;
import org.example.projectcooking.dto.slot.SlotCreateRequest;
import org.example.projectcooking.dto.slot.SlotDetailsResponse;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.BookingMapper;
import org.example.projectcooking.mapper.SlotMapper;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ChefProfileRepository;
import org.example.projectcooking.repository.ProgramRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.example.projectcooking.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Слоты расписания (ФТ-03/04/20/24, D-04/D-09/D-10, UC-03/07). */
@Service
@RequiredArgsConstructor
public class SlotService {

    /** Лимиты вместимости (ФТ-02): 12 по умолчанию, 8 при сложном оборудовании. */
    public static final int CAPACITY_DEFAULT = 12;
    public static final int CAPACITY_COMPLEX = 8;
    private static final int DEFAULT_HORIZON_DAYS = 7;

    private final SlotRepository slotRepository;
    private final ProgramRepository programRepository;
    private final ChefProfileRepository chefProfileRepository;
    private final BookingRepository bookingRepository;
    private final SlotMapper slotMapper;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;

    /**
     * Витрина/сетка (GET /slots, ФТ-20). Гостю и клиенту по умолчанию — только SCHEDULED;
     * ADMIN без фильтра статуса видит все. Период по умолчанию — 7 дней от текущего момента.
     */
    @Transactional(readOnly = true)
    public List<SlotSummaryResponse> listSlots(LocalDate dateFrom, LocalDate dateTo, SlotStatus status, boolean admin) {
        Instant from = dateFrom != null ? dateFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.now();
        Instant to = dateTo != null
                ? dateTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : from.plus(DEFAULT_HORIZON_DAYS, ChronoUnit.DAYS);

        List<Slot> slots;
        if (status != null) {
            slots = slotRepository.findByStartAtGreaterThanEqualAndStartAtLessThanAndStatusOrderByStartAtAsc(from, to, status);
        } else if (admin) {
            slots = slotRepository.findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(from, to);
        } else {
            slots = slotRepository.findByStartAtGreaterThanEqualAndStartAtLessThanAndStatusOrderByStartAtAsc(from, to, SlotStatus.SCHEDULED);
        }
        return slots.stream().map(slotMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public SlotDetailsResponse getDetails(UUID slotId) {
        return slotMapper.toDetails(load(slotId));
    }

    /** Создание слота (ФТ-03). Длительность фиксирована 180 мин; проверки вместимости и D-10. */
    @Transactional
    public SlotDetailsResponse createSlot(SlotCreateRequest req) {
        if (req.getStartAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("START_IN_PAST", "Начало класса не может быть в прошлом");
        }
        Program program = programRepository.findById(req.getProgramId())
                .orElseThrow(() -> ApiException.notFound("Программа не найдена"));
        ChefProfile chef = chefProfileRepository.findById(req.getChefId())
                .orElseThrow(() -> ApiException.notFound("Шеф не найден"));

        int capacityLimit = program.isRequiresComplexEquipment() ? CAPACITY_COMPLEX : CAPACITY_DEFAULT;
        if (req.getCapacityTotal() > capacityLimit) {
            throw ApiException.badRequest("CAPACITY_EXCEEDED",
                    "Вместимость не выше " + capacityLimit + " для этой программы");
        }

        // D-10 (КП-7): пересечение с SCHEDULED-слотом того же шефа в окне (start−180, start+180).
        Instant lower = req.getStartAt().minus(Slot.FIXED_DURATION_MINUTES, ChronoUnit.MINUTES);
        Instant upper = req.getStartAt().plus(Slot.FIXED_DURATION_MINUTES, ChronoUnit.MINUTES);
        List<Slot> conflicts = slotRepository
                .findByChef_UserIdAndStatusAndStartAtGreaterThanAndStartAtLessThan(
                        chef.getUserId(), SlotStatus.SCHEDULED, lower, upper);
        if (!conflicts.isEmpty()) {
            throw ApiException.conflict("CHEF_SLOT_OVERLAP",
                    "Слот пересекается по времени с другим классом шефа",
                    Map.of("conflictingSlotId", conflicts.get(0).getId()));
        }

        int rentalSets = req.getRentalSetsAvailable() == null ? 0 : req.getRentalSetsAvailable();
        Slot slot = Slot.builder()
                .program(program)
                .chef(chef)
                .startAt(req.getStartAt())
                .durationMinutes(Slot.FIXED_DURATION_MINUTES)
                .capacityTotal(req.getCapacityTotal())
                .freeSpots(req.getCapacityTotal())
                .status(SlotStatus.SCHEDULED)
                .rentalSetsAvailable(rentalSets)
                .rentalPricePerSet(req.getRentalPricePerSet())
                .build();
        return slotMapper.toDetails(slotRepository.save(slot));
    }

    /** Отмена слота с причиной (UC-03, ФТ-04). Каскад CONFIRMED → CANCELLED_BY_STUDIO + EML-02. */
    @Transactional
    public SlotDetailsResponse cancelSlot(UUID slotId, String reason) {
        Slot slot = load(slotId);
        if (slot.getStatus() != SlotStatus.SCHEDULED) {
            throw ApiException.conflict("SLOT_NOT_SCHEDULED", "Слот уже не в статусе «запланирован»");
        }
        slot.setStatus(SlotStatus.CANCELLED_BY_STUDIO);
        slot.setCancellationReason(reason);

        List<Booking> confirmed = bookingRepository.findBySlot_IdAndStatus(slotId, BookingStatus.CONFIRMED);
        for (Booking booking : confirmed) {
            booking.setStatus(BookingStatus.CANCELLED_BY_STUDIO);
            // EML-02 — best-effort, сбой не откатывает отмену (НФТ-03/D-07).
            emailService.sendSlotCancellation(
                    booking.getClient().getUser().getEmail(), slot.getProgram().getTitle(), reason);
        }
        slotRepository.save(slot);
        return slotMapper.toDetails(slot);
    }

    /** Участники класса (CHEF своего слота / ADMIN). Тексты отзывов не входят (Р-07). */
    @Transactional(readOnly = true)
    public List<ParticipantBookingResponse> listSlotBookings(UUID slotId, AuthPrincipal principal) {
        Slot slot = load(slotId);
        if (principal.role() == UserRole.CHEF && !slot.getChef().getUserId().equals(principal.userId())) {
            throw ApiException.forbidden("Вы не ведёте этот класс");
        }
        return bookingRepository.findBySlot_Id(slotId).stream().map(bookingMapper::toParticipant).toList();
    }

    /** Классы шефа (GET /chef/slots). period=UPCOMING|PAST или все. */
    @Transactional(readOnly = true)
    public List<SlotSummaryResponse> listChefSlots(UUID chefId, String period) {
        Instant now = Instant.now();
        return slotRepository.findByChef_UserIdOrderByStartAtAsc(chefId).stream()
                .filter(s -> period == null
                        || ("UPCOMING".equals(period) ? !s.getStartAt().isBefore(now) : s.getStartAt().isBefore(now)))
                .map(slotMapper::toSummary)
                .toList();
    }

    private Slot load(UUID id) {
        return slotRepository.findById(id).orElseThrow(() -> ApiException.notFound("Слот не найден"));
    }
}
