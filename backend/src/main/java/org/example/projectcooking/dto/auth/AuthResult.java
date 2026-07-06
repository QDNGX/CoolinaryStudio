package org.example.projectcooking.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.dto.user.CurrentUserResponse;

/** Результат успешной верификации кода: bearer-токен + данные пользователя. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** true — аккаунт клиента только что создан молчаливо (ФТ-21). */
    @JsonProperty("isNewUser")
    private boolean isNewUser;

    private CurrentUserResponse user;
}
