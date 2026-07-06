package org.example.projectcooking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.projectcooking.service.TokenStore;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Аутентификация по непрозрачному bearer-токену: {@code Authorization: Bearer <token>} →
 * {@link TokenStore} → {@link AuthPrincipal} + роль в SecurityContext. Невалидный/отсутствующий
 * токен просто не аутентифицирует запрос (дальше решают правила доступа: 401/403).
 */
@Component
@RequiredArgsConstructor
public class BearerTokenAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            tokenStore.resolve(token).ifPresent(session -> {
                AuthPrincipal principal = new AuthPrincipal(session.userId(), session.role());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, token, List.of(new SimpleGrantedAuthority(principal.authority())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
