package org.example.projectcooking.service;

import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.StudioSettings;
import org.example.projectcooking.dto.studio.StudioSettingsDto;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.StudioSettingsMapper;
import org.example.projectcooking.repository.StudioSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Настройки студии — синглтон (ФТ-17). GET публичен, PUT — только ADMIN. */
@Service
@RequiredArgsConstructor
public class StudioSettingsService {

    private final StudioSettingsRepository repository;
    private final StudioSettingsMapper mapper;

    @Transactional(readOnly = true)
    public StudioSettingsDto get() {
        StudioSettings settings = repository.findById(StudioSettings.SINGLETON_ID)
                .orElseThrow(() -> ApiException.notFound("Настройки студии ещё не заданы"));
        return mapper.toDto(settings);
    }

    @Transactional
    public StudioSettingsDto update(StudioSettingsDto dto) {
        StudioSettings settings = repository.findById(StudioSettings.SINGLETON_ID)
                .orElseGet(() -> StudioSettings.builder().id(StudioSettings.SINGLETON_ID).build());
        settings.setAddress(dto.getAddress());
        settings.setContactPhone(dto.getContactPhone());
        settings.setContactEmail(dto.getContactEmail());
        return mapper.toDto(repository.save(settings));
    }
}
