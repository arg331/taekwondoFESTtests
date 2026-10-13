package com.taekwondo.examenes.club;

import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRole;

/** Miembro de un club, sin email: lo ven el administrador y los profesores del club. */
public record ClubMemberResponse(Long id, String username, String displayName, UserRole role) {

    public static ClubMemberResponse from(User user) {
        return new ClubMemberResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }
}
