package org.example.projectcooking.dto.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.DifficultyLevel;

/** Создание/редактирование программы (POST/PUT /programs, ADMIN — ФТ-01). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramInput {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String cuisineType;

    @NotNull
    private DifficultyLevel difficultyLevel;

    @NotNull
    private Boolean requiresComplexEquipment;

    private List<String> dishes;

    /** До 10 фото (Р-16). */
    @Size(max = 10, message = "не более 10 фото")
    private List<@NotBlank String> photos;
}
