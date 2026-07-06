package org.example.projectcooking.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Профиль шефа в составе /me (tz-11 §2.3). Агрегаты рейтинга — только для чтения (ФТ-12). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefProfileResponse {

    private String photo;
    private String bio;
    private Double averageRating;
    private int reviewsCount;
}
