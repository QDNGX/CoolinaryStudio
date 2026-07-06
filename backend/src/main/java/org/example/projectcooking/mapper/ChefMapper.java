package org.example.projectcooking.mapper;

import org.example.projectcooking.domain.ChefProfile;
import org.example.projectcooking.dto.chef.ChefPublicResponse;
import org.example.projectcooking.dto.chef.ChefResponse;
import org.springframework.stereotype.Component;

@Component
public class ChefMapper {

    /** Публичная карточка (агрегат рейтинга публичен — Р-07). */
    public ChefPublicResponse toPublic(ChefProfile chef) {
        return ChefPublicResponse.builder()
                .id(chef.getUserId())
                .name(chef.getUser().getName())
                .photo(chef.getPhoto())
                .bio(chef.getBio())
                .averageRating(chef.getAverageRating())
                .reviewsCount(chef.getReviewsCount())
                .build();
    }

    /** Представление для ADMIN (SCR-16) — с email и датой создания. */
    public ChefResponse toChefResponse(ChefProfile chef) {
        return ChefResponse.builder()
                .id(chef.getUserId())
                .name(chef.getUser().getName())
                .photo(chef.getPhoto())
                .bio(chef.getBio())
                .averageRating(chef.getAverageRating())
                .reviewsCount(chef.getReviewsCount())
                .email(chef.getUser().getEmail())
                .createdAt(chef.getUser().getCreatedAt())
                .build();
    }
}
