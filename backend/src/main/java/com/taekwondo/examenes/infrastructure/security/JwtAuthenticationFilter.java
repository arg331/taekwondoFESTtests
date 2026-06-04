package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Filtro de Spring Security que valida el token JWT en cada petición.
 *
 * Comportamiento:
 *  - Lee el header "Authorization: Bearer <token>".
 *  - Si hay token y es válido, rellena SecurityContextHolder con el userId.
 *  - Si NO hay token o es inválido, deja pasar la petición sin autenticar.
 *    La decisión de aceptar o rechazar la toma SecurityConfig según la URL.
 *
 * Decisiones clave:
 *  - El principal almacenado es el userId (Long). Si un controller necesita
 *    role/username, hace la consulta a la BD.
 *  - Lista de authorities vacía: aún no implementamos role-based authorization
 *    a nivel de filtro. Cuando llegue, leeremos el role del User cargado y
 *    construiremos SimpleGrantedAuthority("ROLE_ADMIN")/etc.
 *
 * Hereda de OncePerRequestFilter para garantizar UNA invocación por petición
 * (incluso con forwards internos).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        Optional<Long> userId = extractToken(request)
                .flatMap(jwtTokenProvider::extractUserId);

        userId.ifPresent(id -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            id,                       // principal: userId
                            null,                     // credentials (no las usamos)
                            Collections.emptyList()   // authorities (sin roles por ahora)
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
