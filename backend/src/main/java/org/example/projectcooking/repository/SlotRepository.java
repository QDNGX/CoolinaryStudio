package org.example.projectcooking.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    /** Витрина за период (все статусы) — админ (ФТ-20). */
    List<Slot> findByStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(Instant from, Instant to);

    /** Витрина за период с фильтром статуса — гость/клиент по умолчанию SCHEDULED. */
    List<Slot> findByStartAtGreaterThanEqualAndStartAtLessThanAndStatusOrderByStartAtAsc(
            Instant from, Instant to, SlotStatus status);

    /** Классы шефа — вся история (Р-16), сортировка по времени. */
    List<Slot> findByChef_UserIdOrderByStartAtAsc(UUID chefId);

    /** Слоты, срок завершения которых наступил (D-09). */
    List<Slot> findByStatusAndStartAtLessThanEqualOrderByStartAtAsc(SlotStatus status, Instant latestStartAt);

    /**
     * Пересечение слотов шефа (D-10, КП-7): т.к. длительность фиксирована 180 мин, два слота
     * пересекаются ⇔ |startAt − startAt| &lt; 180 мин, т.е. существующий.startAt ∈ (new−180, new+180).
     * Учитываются только SCHEDULED (отменённые/завершённые не мешают).
     */
    List<Slot> findByChef_UserIdAndStatusAndStartAtGreaterThanAndStartAtLessThan(
            UUID chefId, SlotStatus status, Instant lowerExclusive, Instant upperExclusive);

    /**
     * Чтение слота под пессимистичной блокировкой строки — используется в транзакции брони,
     * чтобы уменьшить окно гонки (в дополнение к @Version, D-03/НФТ-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") UUID id);
}
