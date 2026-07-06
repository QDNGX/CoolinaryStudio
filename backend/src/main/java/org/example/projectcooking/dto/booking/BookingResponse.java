package org.example.projectcooking.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.BookingStatus;
import org.example.projectcooking.domain.enums.EquipmentChoice;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;

/** Бронь глазами клиента (openapi {@code Booking}, SCR-05/06). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private SlotSummaryResponse slot;
    private BookingStatus status;
    private EquipmentChoice equipmentChoice;

    /** Цена проката, зафиксированная в момент брони (D-06); null при OWN. */
    private Double rentalPriceSnapshot;

    private Instant createdAt;
    private Instant cancelledAt;

    /** Вычисляется системой при отмене — slot.startAt − cancelledAt < 6ч (D-01). */
    @JsonProperty("isLateCancellation")
    private Boolean isLateCancellation;

    /** Отзыв уже оставлен (D-05 — один отзыв на бронь). */
    private boolean hasReview;
}
