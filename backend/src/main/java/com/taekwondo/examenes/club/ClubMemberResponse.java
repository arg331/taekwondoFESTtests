package com.taekwondo.examenes.club;

import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRole;

/**
 * Miembro de un club, sin email: lo ven el administrador y los profesores del club. resultsVisible dice si
 * quien mira puede abrir sus resultados; si no, el enlace no se muestra (DE-23).
 */
public record ClubMemberResponse(Long id, String username, String displayName, UserRole role, boolean resultsVisible) {

    public static ClubMemberResponse from(User user, boolean resultsVisible) {
        return new ClubMemberResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                resultsVisible);
    }
}
