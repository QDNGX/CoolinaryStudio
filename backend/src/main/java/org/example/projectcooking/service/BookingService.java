package org.example.projectcooking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.ClientProfile;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.EquipmentChoice;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.dto.booking.BookingCreateRequest;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.booking.ParticipantBookingResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.exception.ClientBlockedException;
import org.example.projectcooking.mapper.BookingMapper;
import org.example.projectcooking.repository.BookingRepository;
import org.example.projectcooking.repository.ClientProfileRepository;
import org.example.projectcooking.repository.ReviewRepository;
import org.example.projectcooking.repository.SlotRepository;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Брони: создание/отмена клиентом (UC-01/02), NO_SHOW шефом и снятие админом (UC-06, D-11). */
@Service
@RequiredArgsConstructor
public class BookingService {

    /** Порог бесплатной отмены — 6 часов (D-01). */
    public static final Duration FREE_CANCEL_WINDOW = Duration.ofHours(6);

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final ReviewRepository reviewRepository;
    private final BookingMapper bookingMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * Создание брони (UC-01). Порядок веток по sequence-диаграмме: 403 (блок) → 410 (отменён) →
     * 409 (мест нет) → 201. Слот читается под пессимистичной блокировкой строки для сериализации
     * конкурентных броней; при конфликте блокировки выполняется одна повторная попытка (D-03/НФТ-02).
     */
    public BookingResponse createBooking(UUID clientId, BookingCreateRequest req) {
        try {
            return createBookingAttempt(clientId, req);
        } catch (ObjectOptimisticLockingFailureException | PessimisticLockingFailureException ex) {
            try {
                return createBookingAttempt(clientId, req);
            } catch (ObjectOptimisticLockingFailureException | PessimisticLockingFailureException retryEx) {
                throw ApiException.conflict("SLOT_JUST_TAKEN", "Место только что заняли");
            }
        }
    }

    private BookingResponse createBookingAttempt(UUID clientId, BookingCreateRequest req) {
        return transactionTemplate.execute(status -> doCreateBooking(clientId, req));
    }

    private BookingResponse doCreateBooking(UUID clientId, BookingCreateRequest req) {
        Instant now = Instant.now();
        ClientProfile client = clientProfileRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("Профиль клиента не найден"));

        client.resetBlockIfExpired(now); // ленивый сброс (D-02, КП-5)
        if (client.isBlocked(now)) {
            throw new ClientBlockedException(client.getBlockedUntil());
        }

        Slot slot = slotRepository.findByIdForUpdate(req.getSlotId())
                .orElseThrow(() -> ApiException.notFound("Слот не найден"));

        if (slot.getStatus() == SlotStatus.CANCELLED_BY_STUDIO) {
            throw ApiException.gone("SLOT_CANCELLED", "Класс отменён студией",
                    Map.of("cancellationReason", slot.getCancellationReason() == null ? "" : slot.getCancellationReason()));
        }
        if (slot.getStatus() != SlotStatus.SCHEDULED) {
            throw ApiException.conflict("SLOT_NOT_AVAILABLE", "Класс уже недоступен для брони");
        }

        boolean rental = req.getEquipmentChoice() == EquipmentChoice.RENTAL;
        if (slot.getFreeSpots() <= 0 || (rental && slot.getRentalSetsAvailable() <= 0)) {
            throw ApiException.conflict("SLOT_FULL", "Места закончились");
        }

        Double snapshot = null;
        if (rental) {
            snapshot = slot.getRentalPricePerSet(); // D-06
            slot.setRentalSetsAvailable(slot.getRentalSetsAvailable() - 1);
        }
        slot.setFreeSpots(slot.getFreeSpots() - 1);

        Booking booking = Booking.builder()
                .client(client)
                .slot(slot)
                .createdAt(now)
                .status(BookingStatus.CONFIRMED)
                .equipmentChoice(req.getEquipmentChoice())
                .rentalPriceSnapshot(snapshot)
                .build();
        bookingRepository.save(booking);
        slotRepository.save(slot);
        return bookingMapper.toResponse(booking, false);
    }

    /** Отмена своей брони (UC-02, ФТ-08). D-01 (isLateCancellation) + D-02 (счётчик/блок). */
    @Transactional
    public BookingResponse cancelBooking(UUID clientId, UUID bookingId) {
        Instant now = Instant.now();
        Booking booking = load(bookingId);
        if (!booking.getClient().getUserId().equals(clientId)) {
            throw ApiException.forbidden("Бронь принадлежит другому клиенту");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("BOOKING_NOT_CONFIRMED", "Эту бронь нельзя отменить");
        }
        Slot slot = booking.getSlot();
        boolean late = Duration.between(now, slot.getStartAt()).compareTo(FREE_CANCEL_WINDOW) < 0;

        booking.setCancelledAt(now);
        booking.setIsLateCancellation(late);
        booking.setStatus(BookingStatus.CANCELLED_BY_CLIENT);

        // Возврат места (D-03, симметрично брони).
        slot.setFreeSpots(slot.getFreeSpots() + 1);
        if (booking.getEquipmentChoice() == EquipmentChoice.RENTAL) {
            slot.setRentalSetsAvailable(slot.getRentalSetsAvailable() + 1);
        }
        if (late) {
            booking.getClient().registerViolation(now); // D-02
        }
        return bookingMapper.toResponse(booking, reviewRepository.existsByBooking_Id(bookingId));
    }

    /** Отметка неявки (UC-06, ФТ-10) — только шеф класса и только после startAt (Р-13). */
    @Transactional
    public ParticipantBookingResponse markNoShow(UUID chefId, UUID bookingId) {
        Instant now = Instant.now();
        Booking booking = load(bookingId);
        Slot slot = booking.getSlot();
        if (!slot.getChef().getUserId().equals(chefId)) {
            throw ApiException.forbidden("Вы не ведёте этот класс");
        }
        if (now.isBefore(slot.getStartAt())) {
            throw ApiException.conflict("CLASS_NOT_STARTED", "Класс ещё не начался");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw ApiException.conflict("BOOKING_INVALID_STATE", "Бронь не в подходящем статусе");
        }
        booking.setStatus(BookingStatus.NO_SHOW);
        booking.getClient().registerViolation(now); // D-02
        return bookingMapper.toParticipant(booking);
    }

    /** Снятие ошибочной неявки (D-11, ФТ-22) — только ADMIN. */
    @Transactional
    public ParticipantBookingResponse revokeNoShow(UUID bookingId) {
        Booking booking = load(bookingId);
        if (booking.getStatus() != BookingStatus.NO_SHOW) {
            throw ApiException.conflict("BOOKING_NOT_NO_SHOW", "Бронь не в статусе NO_SHOW");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        booking.getClient().revokeViolation(); // D-11
        return bookingMapper.toParticipant(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMyBookings(UUID clientId) {
        return bookingRepository.findByClient_UserIdOrderByCreatedAtDesc(clientId).stream()
                .map(b -> bookingMapper.toResponse(b, reviewRepository.existsByBooking_Id(b.getId())))
                .toList();
    }

    private Booking load(UUID id) {
        return bookingRepository.findById(id).orElseThrow(() -> ApiException.notFound("Бронь не найдена"));
    }
}
