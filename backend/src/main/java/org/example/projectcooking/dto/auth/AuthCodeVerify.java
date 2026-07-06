package org.example.projectcooking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Проверка кода и открытие сессии (POST /auth/code/verify). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthCodeVerify {

    @NotBlank
    @Email
    private String email;

    /** Одноразовый шестизначный код (EML-01, ФТ-13). */
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "код должен состоять из 6 цифр")
    private String code;
}
