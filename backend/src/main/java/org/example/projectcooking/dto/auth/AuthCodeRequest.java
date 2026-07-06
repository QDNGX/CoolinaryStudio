package org.example.projectcooking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Запрос одноразового кода на email (POST /auth/code/request). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthCodeRequest {

    @NotBlank
    @Email
    private String email;
}
