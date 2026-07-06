package org.example.projectcooking.dto.program;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.DifficultyLevel;

/** Программа каталога (openapi {@code Program}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramResponse {

    private UUID id;
    private String title;
    private String description;
    private String cuisineType;
    private DifficultyLevel difficultyLevel;
    private boolean requiresComplexEquipment;
    private List<String> dishes;
    private List<String> photos;
}
