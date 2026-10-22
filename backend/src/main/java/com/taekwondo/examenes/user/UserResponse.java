package com.taekwondo.examenes.user;

import com.taekwondo.examenes.club.ClubResponse;

import java.time.LocalDateTime;

/** mustChangePassword: tiene una contraseña temporal y debe cambiarla antes de usar la app (RF-33). */
public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        ClubResponse club,
        boolean active,
        boolean mustChangePassword,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRole(), ClubResponse.from(user.getClub()), user.isActive(),
                user.isMustChangePassword(), user.getCreatedAt());
    }
}
