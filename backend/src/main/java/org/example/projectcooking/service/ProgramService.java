package org.example.projectcooking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.Program;
import org.example.projectcooking.dto.program.ProgramInput;
import org.example.projectcooking.dto.program.ProgramResponse;
import org.example.projectcooking.exception.ApiException;
import org.example.projectcooking.mapper.ProgramMapper;
import org.example.projectcooking.repository.ProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Каталог программ (ФТ-01). Чтение — всем; создание/редактирование — только ADMIN. */
@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;

    @Transactional(readOnly = true)
    public List<ProgramResponse> list() {
        return programRepository.findAllByOrderByTitleAsc().stream().map(programMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProgramResponse get(UUID id) {
        return programMapper.toResponse(load(id));
    }

    @Transactional
    public ProgramResponse create(ProgramInput in) {
        Program program = Program.builder()
                .title(in.getTitle())
                .description(in.getDescription())
                .cuisineType(in.getCuisineType())
                .difficultyLevel(in.getDifficultyLevel())
                .requiresComplexEquipment(in.getRequiresComplexEquipment())
                .dishes(new ArrayList<>(in.getDishes() == null ? List.of() : in.getDishes()))
                .photos(new ArrayList<>(in.getPhotos() == null ? List.of() : in.getPhotos()))
                .build();
        return programMapper.toResponse(programRepository.save(program));
    }

    @Transactional
    public ProgramResponse update(UUID id, ProgramInput in) {
        Program program = load(id);
        program.setTitle(in.getTitle());
        program.setDescription(in.getDescription());
        program.setCuisineType(in.getCuisineType());
        program.setDifficultyLevel(in.getDifficultyLevel());
        // Смена requiresComplexEquipment НЕ трогает вместимость уже созданных слотов (Р-04).
        program.setRequiresComplexEquipment(in.getRequiresComplexEquipment());
        program.setDishes(new ArrayList<>(in.getDishes() == null ? List.of() : in.getDishes()));
        program.setPhotos(new ArrayList<>(in.getPhotos() == null ? List.of() : in.getPhotos()));
        return programMapper.toResponse(programRepository.save(program));
    }

    private Program load(UUID id) {
        return programRepository.findById(id).orElseThrow(() -> ApiException.notFound("Программа не найдена"));
    }
}
