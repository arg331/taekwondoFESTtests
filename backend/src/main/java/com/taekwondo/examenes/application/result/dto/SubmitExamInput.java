package com.taekwondo.examenes.application.result.dto;

import java.util.List;

/**
 * Datos para que un estudiante envíe sus respuestas a un examen.
 *
 * El examen se identifica por código (el del QR), no por ID interno.
 * Esto refleja la realidad: el estudiante accede con el código, no
 * tiene acceso a IDs internos.
 *
 * timeSpentSeconds: cuánto tardó. El frontend lo mide localmente.
 *
 * Nota: aquí no hay requesterOwnerId. Los estudiantes son anónimos,
 * no profesores autenticados.
 */
public record SubmitExamInput(
        String examCode,
        String studentName,
        String studentClub,        // opcional
        String studentEmail,       // opcional
        List<AnswerSubmission> answers,
        int timeSpentSeconds
) {}
