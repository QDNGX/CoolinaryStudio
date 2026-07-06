package org.example.projectcooking.mapper;

import org.example.projectcooking.domain.Review;
import org.example.projectcooking.dto.review.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .chefRating(r.getChefRating())
                .programRating(r.getProgramRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
