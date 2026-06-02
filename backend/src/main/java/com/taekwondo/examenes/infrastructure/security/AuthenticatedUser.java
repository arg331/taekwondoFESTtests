package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper para acceder al userId del usuario autenticado en la petición actual.
 *
 * Lee el principal puesto por JwtAuthenticationFilter en SecurityContextHolder.
 * Si no hay autenticación válida, lanza InvalidCredentialsException → 401.
 *
 * Métodos estáticos porque SecurityContextHolder es estático: no hay estado
 * que inyectar.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {}

    /**
     * Devuelve el userId del usuario autenticado.
     * Lanza InvalidCredentialsException si no hay usuario autenticado.
     */
    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InvalidCredentialsException();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        throw new InvalidCredentialsException();
    }
}
