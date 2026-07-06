package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.domain.enums.SlotStatus;
import org.example.projectcooking.domain.enums.UserRole;
import org.example.projectcooking.dto.booking.ParticipantBookingResponse;
import org.example.projectcooking.dto.slot.SlotCancelRequest;
import org.example.projectcooking.dto.slot.SlotCreateRequest;
import org.example.projectcooking.dto.slot.SlotDetailsResponse;
import org.example.projectcooking.dto.slot.SlotSummaryResponse;
import org.example.projectcooking.security.AuthPrincipal;
import org.example.projectcooking.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Слоты расписания: витрина/карточка (гость), управление (ADMIN), кабинет шефа (CHEF). */
@RestController
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /** Витрина/сетка. Гость/клиент — только SCHEDULED; ADMIN без status — все статусы (ФТ-20). */
    @GetMapping("/slots")
    public List<SlotSummaryResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) SlotStatus status,
            @AuthenticationPrincipal AuthPrincipal principal) {
        boolean admin = principal != null && principal.role() == UserRole.ADMIN;
        return slotService.listSlots(dateFrom, dateTo, status, admin);
    }

    @GetMapping("/slots/{slotId}")
    public SlotDetailsResponse get(@PathVariable UUID slotId) {
        return slotService.getDetails(slotId);
    }

    @PostMapping("/slots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlotDetailsResponse> create(@Valid @RequestBody SlotCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.createSlot(request));
    }

    @PostMapping("/slots/{slotId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public SlotDetailsResponse cancel(@PathVariable UUID slotId, @Valid @RequestBody SlotCancelRequest request) {
        return slotService.cancelSlot(slotId, request.getReason());
    }

    @GetMapping("/slots/{slotId}/bookings")
    @PreAuthorize("hasAnyRole('CHEF','ADMIN')")
    public List<ParticipantBookingResponse> bookings(@PathVariable UUID slotId,
                                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return slotService.listSlotBookings(slotId, principal);
    }

    @GetMapping("/chef/slots")
    @PreAuthorize("hasRole('CHEF')")
    public List<SlotSummaryResponse> chefSlots(@RequestParam(required = false) String period,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        return slotService.listChefSlots(principal.userId(), period);
    }
}
