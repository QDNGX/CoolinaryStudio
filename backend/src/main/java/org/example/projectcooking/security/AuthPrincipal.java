package org.example.projectcooking.security;

import java.util.UUID;
import org.example.projectcooking.domain.enums.UserRole;

/** Аутентифицированный субъект, положенный в SecurityContext фильтром bearer-токена. */
public record AuthPrincipal(UUID userId, UserRole role) {

    public String authority() {
        return "ROLE_" + role.name();
    }
}
