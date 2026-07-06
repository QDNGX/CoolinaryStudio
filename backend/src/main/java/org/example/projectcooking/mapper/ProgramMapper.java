package org.example.projectcooking.mapper;

import java.util.ArrayList;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.dto.program.ProgramResponse;
import org.springframework.stereotype.Component;

@Component
public class ProgramMapper {

    public ProgramResponse toResponse(Program p) {
        return ProgramResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .cuisineType(p.getCuisineType())
                .difficultyLevel(p.getDifficultyLevel())
                .requiresComplexEquipment(p.isRequiresComplexEquipment())
                .dishes(new ArrayList<>(p.getDishes()))
                .photos(new ArrayList<>(p.getPhotos()))
                .build();
    }
}
