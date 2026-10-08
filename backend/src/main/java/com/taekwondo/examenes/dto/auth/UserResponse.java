package com.taekwondo.examenes.dto.auth;

import com.taekwondo.examenes.entity.User;
import com.taekwondo.examenes.entity.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
