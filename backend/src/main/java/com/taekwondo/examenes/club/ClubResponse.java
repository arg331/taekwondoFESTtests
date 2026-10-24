package com.taekwondo.examenes.club;

/** Club tal como se lista (también sin sesión, para el registro) y como aparece en el usuario. */
public record ClubResponse(Long id, String name) {

    public static ClubResponse from(Club club) {
        return club == null ? null : new ClubResponse(club.getId(), club.getName());
    }
}
