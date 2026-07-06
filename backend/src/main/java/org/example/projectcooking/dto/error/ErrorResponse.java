package org.example.projectcooking.dto.error;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Тело ошибки по контракту openapi ({@code Error}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** Машиночитаемый код: INVALID_CODE, SLOT_FULL, SLOT_JUST_TAKEN, CHEF_SLOT_OVERLAP … */
    private String code;

    /** Человекочитаемое сообщение для UI. */
    private String message;

    /** Доп. контекст: conflictingSlotId, cancellationReason, blockedUntil … */
    private Map<String, Object> details;
}
