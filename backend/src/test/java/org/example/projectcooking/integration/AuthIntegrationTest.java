package org.example.projectcooking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.example.projectcooking.dto.auth.AuthResult;
import org.example.projectcooking.dto.user.CurrentUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Transactional
@Import(IntegrationTestConfig.class)
class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void requestVerifyAndLogoutWorkEndToEnd() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.local";

        AuthResult authResult = authenticate(email);

        assertThat(authResult.getTokenType()).isEqualTo("Bearer");
        assertThat(authResult.isNewUser()).isTrue();
        assertThat(authResult.getAccessToken()).isNotBlank();
        assertThat(authResult.getUser()).isNotNull();
        CurrentUserResponse user = authResult.getUser();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole().name()).isEqualTo("CLIENT");

        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(authResult.getAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CLIENT"));

        mockMvc.perform(post("/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(authResult.getAccessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(authResult.getAccessToken())))
                .andExpect(status().isUnauthorized());
    }
}
