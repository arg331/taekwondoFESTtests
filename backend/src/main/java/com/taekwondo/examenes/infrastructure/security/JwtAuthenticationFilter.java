package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import com.taekwondo.examenes.domain.port.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro JWT actualizado: carga el usuario de la BD y añade su rol
 * como authority para que SecurityConfig pueda hacer .hasAuthority("ADMIN").
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER   = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository   userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                    UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository   = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        extractToken(request)
                .flatMap(jwtTokenProvider::extractUserId)
                .flatMap(userRepository::findById)
                .filter(User::isActive)
                .ifPresent(user -> {
                    var authority = new SimpleGrantedAuthority(user.getRole().name());
                    var auth = new UsernamePasswordAuthenticationToken(
                            user.getId(),
                            null,
                            List.of(authority)
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

        chain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) return Optional.empty();
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
