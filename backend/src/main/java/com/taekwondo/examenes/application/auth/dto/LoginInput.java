package com.taekwondo.examenes.application.auth.dto;

/**
 * Datos para iniciar sesión.
 *
 * usernameOrEmail: aceptamos ambos como identificador. El caso de uso
 * intenta primero por username, después por email.
 */
public record LoginInput(
        String usernameOrEmail,
        String plainPassword
) {}
