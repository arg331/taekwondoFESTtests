package com.taekwondo.examenes.club;

import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRole;

/** Usuario que se puede añadir a un club; clubName es el club en el que está ahora (solo lo ve el administrador). */
public record ClubCandidateResponse(Long id, String username, String displayName, UserRole role, String clubName) {

    public static ClubCandidateResponse from(User user) {
        return new ClubCandidateResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
                user.getClub() == null ? null : user.getClub().getName());
    }
}
