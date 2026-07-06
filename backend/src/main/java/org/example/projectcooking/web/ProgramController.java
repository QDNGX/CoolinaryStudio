package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.program.ProgramInput;
import org.example.projectcooking.dto.program.ProgramResponse;
import org.example.projectcooking.service.ProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Каталог программ (ФТ-01). Чтение — всем/гостю; изменение — только ADMIN. */
@RestController
@RequestMapping("/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    public List<ProgramResponse> list() {
        return programService.list();
    }

    @GetMapping("/{programId}")
    public ProgramResponse get(@PathVariable UUID programId) {
        return programService.get(programId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramResponse> create(@Valid @RequestBody ProgramInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.create(input));
    }

    @PutMapping("/{programId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse update(@PathVariable UUID programId, @Valid @RequestBody ProgramInput input) {
        return programService.update(programId, input);
    }
}
