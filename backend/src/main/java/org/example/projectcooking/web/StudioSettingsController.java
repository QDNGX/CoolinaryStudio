package org.example.projectcooking.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.studio.StudioSettingsDto;
import org.example.projectcooking.service.StudioSettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Настройки студии — синглтон (ФТ-17). GET публичен, PUT — ADMIN. */
@RestController
@RequestMapping("/studio-settings")
@RequiredArgsConstructor
public class StudioSettingsController {

    private final StudioSettingsService studioSettingsService;

    @GetMapping
    public StudioSettingsDto get() {
        return studioSettingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public StudioSettingsDto update(@Valid @RequestBody StudioSettingsDto dto) {
        return studioSettingsService.update(dto);
    }
}
