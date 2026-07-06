package org.example.projectcooking.mapper;

import org.example.projectcooking.domain.StudioSettings;
import org.example.projectcooking.dto.studio.StudioSettingsDto;
import org.springframework.stereotype.Component;

@Component
public class StudioSettingsMapper {

    public StudioSettingsDto toDto(StudioSettings s) {
        return StudioSettingsDto.builder()
                .address(s.getAddress())
                .contactPhone(s.getContactPhone())
                .contactEmail(s.getContactEmail())
                .build();
    }
}
