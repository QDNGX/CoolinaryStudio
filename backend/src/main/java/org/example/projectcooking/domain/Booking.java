package org.example.projectcooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.EquipmentChoice;

/**
 * Бронь клиента на слот (tz-11 §2.7). rentalPriceSnapshot фиксируется при RENTAL и не меняется (D-06).
 * isLateCancellation вычисляет только система при отмене (D-01).
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientProfile client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentChoice equipmentChoice;

    /** Копия Slot.rentalPricePerSet в момент брони при RENTAL; null при OWN (D-06). */
    private Double rentalPriceSnapshot;

    private Instant cancelledAt;

    /** derived: slot.startAt − cancelledAt < 6ч (D-01). */
    private Boolean isLateCancellation;

    /** Идемпотентность EML-03: напоминание за 24 часа отправляется максимум один раз. */
    private Instant reminder24hSentAt;

    /** Идемпотентность EML-04: напоминание за 2 часа отправляется максимум один раз. */
    private Instant reminder2hSentAt;
}
