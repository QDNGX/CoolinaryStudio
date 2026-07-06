package org.example.projectcooking.domain.enums;

/**
 * Жизненный цикл брони (ФТ-19). Каждый актор меняет строго свой кусок (матрица доступа).
 */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED_BY_CLIENT,
    CANCELLED_BY_STUDIO,
    COMPLETED,
    NO_SHOW
}
