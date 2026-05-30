package com.taekwondo.examenes.application.exam.dto;

import com.taekwondo.examenes.domain.model.Visibility;

/**
 * Datos para cambiar la visibilidad de un examen ya publicado o expirado.
 *
 * No aplicable a drafts: un draft no tiene visibilidad pública (no existe
 * fuera del profesor que lo crea). La entidad valida esto.
 */
public record ChangeExamVisibilityInput(
        Long examId,
        Visibility newVisibility,
        Long requesterOwnerId
) {}