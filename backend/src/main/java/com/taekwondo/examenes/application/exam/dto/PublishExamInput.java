package com.taekwondo.examenes.application.exam.dto;

import com.taekwondo.examenes.domain.model.Visibility;

import java.time.LocalDateTime;

/**
 * Datos para publicar un examen.
 *
 * expiresAt es OPCIONAL (null = aplicar default de +1 hora desde la
 * publicación).
 *
 * visibility define si el examen es público o privado tras la publicación.
 * Esto NO afecta el acceso de estudiantes (que es siempre por código), solo
 * afecta si otros profesores pueden ver/favoritear este examen.
 */
public record PublishExamInput(
        Long examId,
        Visibility visibility,
        LocalDateTime expiresAt,
        Long requesterOwnerId
) {}