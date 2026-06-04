package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.application.shared.exception.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper para acceder al userId del usuario autenticado en la petición actual.
 *
 * Lee el principal puesto por JwtAuthenticationFilter en SecurityContextHolder.
 *
 * Dos modos:
 *  - currentUserId(): lanza InvalidCredentialsException si no hay user.
 *    Para endpoints que requieren auth obligatoriamente.
 *  - currentUserIdOrNull(): devuelve null si no hay user.
 *    Para endpoints públicos que opcionalmente identifican al usuario
 *    (típicamente POST /api/results, donde anónimos y registrados
 *    pueden enviar resultados pero solo los registrados se identifican).
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {}

    /**
     * Devuelve el userId del usuario autenticado.
     * Lanza InvalidCredentialsException si no hay usuario autenticado.
     */
    public static Long currentUserId() {
        Long id = currentUserIdOrNull();
        if (id == null) {
            throw new InvalidCredentialsException();
        }
        return id;
    }

    /**
     * Devuelve el userId del usuario autenticado, o null si no hay
     * autenticación válida (anónimo, sin token, token inválido).
     *
     * Útil para endpoints públicos que opcionalmente identifican
     * al usuario sin exigirlo.
     */
    public static Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Long id) ? id : null;
    }
}
