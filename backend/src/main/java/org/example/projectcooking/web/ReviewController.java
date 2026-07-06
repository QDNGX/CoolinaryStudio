package org.example.projectcooking.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.dto.review.ReviewCreateRequest;
import org.example.projectcooking.dto.review.ReviewResponse;
import org.example.projectcooking.security.AuthPrincipal;
import org.example.projectcooking.service.ReviewService;
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

/** Отзывы по завершённым броням (UC-04, D-05); тексты — только ADMIN (Р-07). */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/bookings/{bookingId}/review")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReviewResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable UUID bookingId,
                                                 @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.create(principal.userId(), bookingId, request));
    }

    @GetMapping("/bookings/{bookingId}/review")
    @PreAuthorize("hasRole('CLIENT')")
    public ReviewResponse getMyReview(@AuthenticationPrincipal AuthPrincipal principal,
                                      @PathVariable UUID bookingId) {
        return reviewService.getByBooking(principal.userId(), bookingId);
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewResponse> list(@RequestParam(required = false) UUID chefId,
                                     @RequestParam(required = false) UUID programId) {
        return reviewService.listReviews(chefId, programId);
    }
}
