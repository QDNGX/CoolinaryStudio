package org.example.projectcooking.domain.enums;

/**
 * Жизненный цикл слота (ФТ-19).
 * SCHEDULED → CANCELLED_BY_STUDIO (ADMIN, ФТ-04) или SCHEDULED → COMPLETED (система, D-09).
 */
public enum SlotStatus {
    SCHEDULED,
    CANCELLED_BY_STUDIO,
    COMPLETED
}
