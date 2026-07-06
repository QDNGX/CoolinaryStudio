package org.example.projectcooking.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Отзыв по завершённой брони (POST /bookings/{id}/review, CLIENT — UC-04). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer chefRating;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer programRating;

    /** Необязательный комментарий, ≤ 1000 (Р-16); текст видит только ADMIN (Р-07). */
    @Size(max = 1000, message = "комментарий не длиннее 1000 символов")
    private String comment;
}
