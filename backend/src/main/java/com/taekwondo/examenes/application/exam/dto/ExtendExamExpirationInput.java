package com.taekwondo.examenes.application.exam.dto;

import java.time.LocalDateTime;

/**
 * Datos para extender la fecha de expiración de un examen ya publicado.
 *
 * Diferencia con ReopenExamUseCase: aquí el examen sigue PUBLISHED, no
 * pasa por EXPIRED. Se usa cuando el examen todavía está activo pero
 * el profesor quiere darle más tiempo.
 *
 * newExpiresAt: puede ser null (eliminar el límite temporal = examen sin
 * caducidad automática).
 */
public record ExtendExamExpirationInput(
        Long examId,
        LocalDateTime newExpiresAt,
        Long requesterOwnerId
) {}
