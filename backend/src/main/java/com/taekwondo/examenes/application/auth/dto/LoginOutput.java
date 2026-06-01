package com.taekwondo.examenes.application.auth.dto;

/**
 * Resultado de un login exitoso: token JWT y datos del usuario.
 *
 * Devolver ambos en la misma respuesta evita que el frontend tenga que
 * hacer un segundo /me tras el login.
 */
public record LoginOutput(
        String token,
        UserView user
) {}
