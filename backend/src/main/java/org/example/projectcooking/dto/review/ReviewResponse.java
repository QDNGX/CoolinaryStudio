package org.example.projectcooking.dto.review;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отзыв (openapi {@code Review}). Комментарий: клиенту — свой; полный доступ — только ADMIN (Р-07). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID bookingId;
    private int chefRating;
    private int programRating;
    private String comment;
    private Instant createdAt;
}
