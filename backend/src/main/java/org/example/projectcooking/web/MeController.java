package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.booking.BookingResponse;
import org.example.projectcooking.dto.user.CurrentUserResponse;
import org.example.projectcooking.dto.user.UpdateMeRequest;
import org.example.projectcooking.security.AuthPrincipal;
import org.example.projectcooking.service.BookingService;
import org.example.projectcooking.service.MeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Профиль текущего пользователя и его брони (SCR-08, SCR-06). */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;
    private final BookingService bookingService;

    @GetMapping("/me")
    public CurrentUserResponse getMe(@AuthenticationPrincipal AuthPrincipal principal) {
        return meService.getMe(principal.userId());
    }

    @PatchMapping("/me")
    public CurrentUserResponse updateMe(@AuthenticationPrincipal AuthPrincipal principal,
                                        @Valid @RequestBody UpdateMeRequest request) {
        return meService.updateMe(principal.userId(), request);
    }

    @GetMapping("/me/bookings")
    @PreAuthorize("hasRole('CLIENT')")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthPrincipal principal) {
        return bookingService.listMyBookings(principal.userId());
    }
}
