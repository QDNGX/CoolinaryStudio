package org.example.projectcooking.dto.chef;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Публичная карточка шефа в составе слота (SCR-04); публичен только агрегат рейтинга (Р-07). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefPublicResponse {

    private UUID id;
    private String name;
    private String photo;
    private String bio;
    private Double averageRating;
    private int reviewsCount;
}
