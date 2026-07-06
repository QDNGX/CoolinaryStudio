package org.example.projectcooking.mapper;

import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Slot;
import org.example.projectcooking.dto.slot.SlotDetailsResponse;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlotMapper {

    private final ChefMapper chefMapper;
    private final ProgramMapper programMapper;

    public SlotSummaryResponse toSummary(Slot s) {
        return SlotSummaryResponse.builder()
                .id(s.getId())
                .programId(s.getProgram().getId())
                .programTitle(s.getProgram().getTitle())
                .chef(chefMapper.toPublic(s.getChef()))
                .startAt(s.getStartAt())
                .durationMinutes(s.getDurationMinutes())
                .capacityTotal(s.getCapacityTotal())
                .freeSpots(s.getFreeSpots())
                .status(s.getStatus())
                .cancellationReason(s.getCancellationReason())
                .rentalSetsAvailable(s.getRentalSetsAvailable())
                .rentalPricePerSet(s.getRentalPricePerSet())
                .build();
    }

    public SlotDetailsResponse toDetails(Slot s) {
        return SlotDetailsResponse.builder()
                .summary(toSummary(s))
                .program(programMapper.toResponse(s.getProgram()))
                .build();
    }
}
