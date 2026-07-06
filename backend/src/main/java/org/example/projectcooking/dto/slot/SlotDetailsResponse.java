package org.example.projectcooking.dto.slot;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.dto.program.ProgramResponse;

/**
 * Карточка слота (openapi {@code SlotDetails}, SCR-04) = поля SlotSummary (разворачиваются
 * в корень через {@link JsonUnwrapped}) + полная программа.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotDetailsResponse {

    @JsonUnwrapped
    private SlotSummaryResponse summary;

    private ProgramResponse program;
}
