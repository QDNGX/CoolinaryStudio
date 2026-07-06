package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.booking.BookingCreateRequest;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.booking.ParticipantBookingResponse;
import org.example.projectcooking.security.AuthPrincipal;
import org.example.projectcooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Брони: создание/отмена клиентом, NO_SHOW шефом, снятие NO_SHOW админом (UC-01/02/06). */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(principal.userId(), request));
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public BookingResponse cancel(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID bookingId) {
        return bookingService.cancelBooking(principal.userId(), bookingId);
    }

    @PostMapping("/{bookingId}/no-show")
    @PreAuthorize("hasRole('CHEF')")
    public ParticipantBookingResponse markNoShow(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable UUID bookingId) {
        return bookingService.markNoShow(principal.userId(), bookingId);
    }

    @DeleteMapping("/{bookingId}/no-show")
    @PreAuthorize("hasRole('ADMIN')")
    public ParticipantBookingResponse revokeNoShow(@PathVariable UUID bookingId) {
        return bookingService.revokeNoShow(bookingId);
    }
}
