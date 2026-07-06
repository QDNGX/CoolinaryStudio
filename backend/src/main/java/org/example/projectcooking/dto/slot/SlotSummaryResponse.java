package org.example.projectcooking.dto.slot;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.dto.chef.ChefPublicResponse;

/** Строка витрины расписания (openapi {@code SlotSummary}, SCR-03). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotSummaryResponse {

    private UUID id;
    private UUID programId;
    private String programTitle;
    private ChefPublicResponse chef;
    private Instant startAt;
    private int durationMinutes;
    private int capacityTotal;
    private int freeSpots;
    private SlotStatus status;
    private String cancellationReason;
    private int rentalSetsAvailable;
    private Double rentalPricePerSet;
}
