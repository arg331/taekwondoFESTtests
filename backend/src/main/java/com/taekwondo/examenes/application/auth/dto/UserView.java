package com.taekwondo.examenes.application.auth.dto;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;

import java.time.LocalDateTime;

/**
 * Vista de un usuario.
 *
 * IMPORTANTE: NUNCA incluye el passwordHash. Aunque sea solo el hash y
 * no se pueda recuperar la contraseña, no tiene por qué salir del backend.
 *
 * Incluye el role para que el frontend pueda adaptar la UI:
 *  - ADMIN  → muestra opciones de profesor
 *  - STUDENT → muestra solo opciones de estudiante
 */
public record UserView(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserView from(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
