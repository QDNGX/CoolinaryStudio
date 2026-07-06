package org.example.projectcooking.dto.chef;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Представление шефа для ADMIN (SCR-16) — публичные поля + email/createdAt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefResponse {

    private UUID id;
    private String name;
    private String photo;
    private String bio;
    private Double averageRating;
    private int reviewsCount;
    private String email;
    private Instant createdAt;
}
