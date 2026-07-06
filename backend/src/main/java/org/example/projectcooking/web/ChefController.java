package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.chef.ChefCreateRequest;
import org.example.projectcooking.dto.chef.ChefResponse;
import org.example.projectcooking.dto.chef.ChefUpdateRequest;
import org.example.projectcooking.service.ChefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Аккаунты и профили шефов — ведёт ADMIN (UC-08, ФТ-14/25). */
@RestController
@RequestMapping("/chefs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ChefController {

    private final ChefService chefService;

    @GetMapping
    public List<ChefResponse> list() {
        return chefService.list();
    }

    @PostMapping
    public ResponseEntity<ChefResponse> create(@Valid @RequestBody ChefCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chefService.create(request));
    }

    @PatchMapping("/{chefId}")
    public ChefResponse update(@PathVariable UUID chefId, @Valid @RequestBody ChefUpdateRequest request) {
        return chefService.update(chefId, request);
    }
}
