package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.auth.dto.UserView;
import com.taekwondo.examenes.domain.model.UserRole;

import java.time.LocalDateTime;

/**
 * DTO HTTP de salida con datos públicos de un usuario.
 *
 * NUNCA incluye passwordHash. UserView (aplicación) tampoco lo expone,
 * así que esa garantía viene de varias capas.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(UserView view) {
        return new UserResponse(
                view.id(),
                view.username(),
                view.email(),
                view.displayName(),
                view.role(),
                view.active(),
                view.createdAt()
        );
    }
}
