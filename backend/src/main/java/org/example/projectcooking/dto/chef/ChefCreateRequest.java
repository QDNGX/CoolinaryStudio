package org.example.projectcooking.dto.chef;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Заведение аккаунта шефа по email (POST /chefs, ADMIN — D-08). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChefCreateRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(max = 100)
    private String name;

    private String photo;
    private String bio;
}
