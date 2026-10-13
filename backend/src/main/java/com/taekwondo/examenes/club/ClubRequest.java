package com.taekwondo.examenes.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Crear o renombrar un club. */
public record ClubRequest(@NotBlank @Size(max = Club.MAX_NAME_LENGTH) String name) {}
