package org.example.projectcooking.dto.chef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Редактирование фото и био шефа (PATCH /chefs/{id}, ADMIN — Р-05). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefUpdateRequest {

    private String photo;
    private String bio;
}
