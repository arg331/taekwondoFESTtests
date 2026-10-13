package com.taekwondo.examenes.user;

import com.taekwondo.examenes.club.ClubResponse;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        UserRole role,
        ClubResponse club,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRole(), ClubResponse.from(user.getClub()), user.isActive(),
                user.getCreatedAt());
    }
}
