package org.example.projectcooking.dto.studio;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Настройки студии — синглтон (openapi {@code StudioSettings}, ФТ-17). GET/PUT общий формат. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioSettingsDto {

    @NotBlank
    private String address;

    @NotBlank
    private String contactPhone;

    @NotBlank
    @Email
    private String contactEmail;
}
