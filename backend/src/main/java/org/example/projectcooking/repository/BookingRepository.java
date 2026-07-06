package org.example.projectcooking.repository;

import java.util.List;
import java.util.UUID;
import org.example.projectcooking.domain.Booking;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /** Мои брони (клиент), от новых к старым, вся история (Р-16). */
    List<Booking> findByClient_UserIdOrderByCreatedAtDesc(UUID clientId);

    /** Участники слота (шеф своего класса / админ). */
    List<Booking> findBySlot_Id(UUID slotId);

    /** Брони слота в статусе — для каскадной отмены (CONFIRMED) и автозавершения. */
    List<Booking> findBySlot_IdAndStatus(UUID slotId, BookingStatus status);

    /** EML-03: due-окно для напоминаний за 24 часа, без заднего числа для поздних броней. */
    @Query("""
            select b from Booking b
              join fetch b.client c
              join fetch c.user
              join fetch b.slot s
              join fetch s.program
            where b.status = :bookingStatus
              and s.status = :slotStatus
              and b.reminder24hSentAt is null
              and b.createdAt <= :dueBoundary
              and s.startAt > :windowStart
              and s.startAt <= :windowEnd
            order by s.startAt asc
            """)
    List<Booking> findDue24hReminders(
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("slotStatus") SlotStatus slotStatus,
            @Param("dueBoundary") java.time.Instant dueBoundary,
            @Param("windowStart") java.time.Instant windowStart,
            @Param("windowEnd") java.time.Instant windowEnd);

    /** EML-04: due-окно для напоминаний за 2 часа, без заднего числа для поздних броней. */
    @Query("""
            select b from Booking b
              join fetch b.client c
              join fetch c.user
              join fetch b.slot s
              join fetch s.program
            where b.status = :bookingStatus
              and s.status = :slotStatus
              and b.reminder2hSentAt is null
              and b.createdAt <= :dueBoundary
              and s.startAt > :windowStart
              and s.startAt <= :windowEnd
            order by s.startAt asc
            """)
    List<Booking> findDue2hReminders(
            @Param("bookingStatus") BookingStatus bookingStatus,
            @Param("slotStatus") SlotStatus slotStatus,
            @Param("dueBoundary") java.time.Instant dueBoundary,
            @Param("windowStart") java.time.Instant windowStart,
            @Param("windowEnd") java.time.Instant windowEnd);
}
