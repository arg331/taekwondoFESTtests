package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * DTO HTTP para iniciar sesión.
 *
 * usernameOrEmail acepta ambos identificadores; el caso de uso intenta
 * primero username, luego email.
 */
public record LoginRequest(
        String usernameOrEmail,
        String plainPassword
) {}
