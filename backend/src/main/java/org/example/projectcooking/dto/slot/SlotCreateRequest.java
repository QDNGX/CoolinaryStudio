package org.example.projectcooking.dto.slot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Создание слота (POST /slots, ADMIN — ФТ-03). Длительность фиксирована 180 мин и не передаётся.
 * Вместимость ≤ лимита программы (12, либо 8 при requiresComplexEquipment) — проверяется в сервисе.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotCreateRequest {

    @NotNull
    private UUID programId;

    @NotNull
    private UUID chefId;

    @NotNull
    private Instant startAt;

    @NotNull
    @Min(value = 1, message = "вместимость не меньше 1")
    private Integer capacityTotal;

    @Min(value = 0, message = "прокатный фонд не может быть отрицательным")
    private Integer rentalSetsAvailable;

    @PositiveOrZero(message = "цена проката не может быть отрицательной")
    private Double rentalPricePerSet;

    /** rentalPricePerSet обязателен при rentalSetsAvailable > 0 (openapi SlotCreateRequest). */
    @JsonIgnore
    @AssertTrue(message = "укажите цену проката при наличии прокатного фонда")
    public boolean isRentalPriceConsistent() {
        int sets = rentalSetsAvailable == null ? 0 : rentalSetsAvailable;
        return sets <= 0 || rentalPricePerSet != null;
    }
}
