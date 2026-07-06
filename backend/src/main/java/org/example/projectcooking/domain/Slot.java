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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.projectcooking.domain.enums.SlotStatus;

/**
 * Слот расписания (tz-11 §2.6) — «горячая» сущность: {@link #version} (оптимистичная блокировка, D-03)
 * и {@link #freeSpots} (кэш мест). Редактирования слота нет (Р-03) — исправление = отмена + создание.
 * durationMinutes фиксировано 180 (ФТ-03) и не приходит из API.
 */
@Entity
@Table(name = "slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slot {

    public static final int FIXED_DURATION_MINUTES = 180;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** FK → ChefProfile (chefId = User.id роли CHEF). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chef_id", nullable = false)
    private ChefProfile chef;

    @Column(nullable = false)
    private Instant startAt;

    @Builder.Default
    @Column(nullable = false)
    private int durationMinutes = FIXED_DURATION_MINUTES;

    /** ≥ 1; ≤ 12, либо ≤ 8 при requiresComplexEquipment на момент создания (ФТ-02, Р-04). */
    @Column(nullable = false)
    private int capacityTotal;

    /** Кэш мест — пересчёт строго в транзакции брони/отмены (D-03); только система. */
    @Column(nullable = false)
    private int freeSpots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status;

    /** Обязательна при CANCELLED_BY_STUDIO (ФТ-04); ≤ 300 (Р-16). */
    @Column(length = 300)
    private String cancellationReason;

    @Builder.Default
    @Column(nullable = false)
    private int rentalSetsAvailable = 0;

    /** Текущая цена проката; в брони фиксируется снапшотом (D-06). */
    private Double rentalPricePerSet;

    /** Оптимистичная блокировка — JPA @Version (D-03, НФТ-02). */
    @Version
    private Integer version;
}
