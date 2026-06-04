package com.taekwondo.examenes.infrastructure.web.dto;

import java.util.List;

/**
 * Body de POST /api/results.
 *
 * El examen se identifica por código (el del QR), no por id interno.
 *
 * Datos del estudiante:
 *  - studentName obligatorio
 *  - studentClub y studentEmail opcionales
 *  - studentUserId NO va aquí; el controller lo obtiene del JWT si lo hay,
 *    o lo pasa como null si es anónimo.
 *
 * timeSpentSeconds: lo mide el frontend.
 */
public record SubmitExamRequest(
        String examCode,
        String studentName,
        String studentClub,
        String studentEmail,
        List<AnswerSubmissionRequest> answers,
        int timeSpentSeconds
) {}
