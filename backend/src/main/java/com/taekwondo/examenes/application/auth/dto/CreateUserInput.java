package com.taekwondo.examenes.application.auth.dto;

/**
 * Datos para crear un usuario nuevo (uso interno: admins).
 *
 * La password viene en CLARO. El caso de uso la hashea con PasswordHasher
 * ANTES de construir la entidad User.
 */
public record CreateUserInput(
        String username,
        String email,
        String plainPassword,
        String displayName
) {}
