package com.taekwondo.examenes.domain.port;

import java.util.Optional;

/**
 * Puerto de salida: generación y verificación de tokens JWT.
 *
 * El dominio NO conoce librerías de JWT. Solo conoce el contrato:
 * dado un userId genero un token; dado un token extraigo el userId
 * si es válido.
 */
public interface JwtTokenProvider {

    String generateToken(Long userId);

    /**
     * Devuelve empty si el token es inválido o ha expirado.
     */
    Optional<Long> extractUserId(String token);
}
