package ch.it4user.foodsharing.security;

import ch.it4user.foodsharing.domain.entity.AuthSession;
import ch.it4user.foodsharing.repository.AuthSessionRepository;
import ch.it4user.foodsharing.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSessionRepository authSessionRepository;
    private final TokenService tokenService;

    public BearerTokenAuthenticationFilter(AuthSessionRepository authSessionRepository, TokenService tokenService) {
        this.authSessionRepository = authSessionRepository;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String rawToken = authorization.substring("Bearer ".length()).trim();
            authSessionRepository.findByTokenHash(tokenService.hash(rawToken))
                    .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                    .ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AuthSession session) {
        var authorities = Arrays.stream(session.getPermissions().split(","))
                .filter(permission -> !permission.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(session.getFoodsharingId(), session.getTokenHash(), authorities));
    }
}
