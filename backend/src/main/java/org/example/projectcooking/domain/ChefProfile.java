package org.example.projectcooking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Расширение User для role=CHEF (1:1, общий PK) — tz-11 §2.3.
 * photo/bio редактирует только ADMIN (Р-05); averageRating/reviewsCount — производные (ФТ-12).
 */
@Entity
@Table(name = "chef_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefProfile {

    @Id
    private UUID userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** URL фото; редактирует ADMIN (Р-05). */
    private String photo;

    /** Био; редактирует ADMIN (Р-05). */
    @Column(length = 2000)
    private String bio;

    /** Агрегат по Review.chefRating (ФТ-12); null пока отзывов нет; только система. */
    private Double averageRating;

    @Builder.Default
    @Column(nullable = false)
    private int reviewsCount = 0;
}
