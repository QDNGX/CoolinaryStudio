package org.example.projectcooking.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.projectcooking.domain.enums.UserRole;

/** Данные текущего пользователя (GET /me, часть AuthResult). SCR-08. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentUserResponse {

    private UUID id;
    private UserRole role;
    private String name;
    private String email;
    private boolean enabled;
    private Instant createdAt;

    /** Присутствует только для role=CLIENT. */
    private ClientProfileResponse clientProfile;

    /** Присутствует только для role=CHEF. */
    private ChefProfileResponse chefProfile;
}
