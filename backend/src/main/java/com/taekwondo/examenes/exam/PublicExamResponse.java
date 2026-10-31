package com.taekwondo.examenes.exam;

import java.time.LocalDateTime;

/**
 * Un examen visto por el alumno, antes de empezarlo: la portada para quien tiene el código (DE-19) y las
 * tarjetas de «Exámenes de prueba» (DE-22). Sin ids del profesor ni de las preguntas, tags ni configuración interna.
 */
public record PublicExamResponse(
        String title,
        String code,
        /** De prueba (público) u oficial (privado), RF-49. */
        Visibility visibility,
        /** Si hace falta cuenta para hacerlo (DE-06). */
        ExamAccessMode accessMode,
        Config config,
        /** Intentos por alumno; null = sin límite (RF-52). */
        Integer maxAttempts,
        /** Hasta cuándo se puede empezar; null = hasta que el profesor lo cierre (RF-48). */
        LocalDateTime expiresAt
) {
    public record Config(int numberOfQuestions, Integer timeLimitMinutes) {}

    static PublicExamResponse from(Exam exam) {
        return new PublicExamResponse(exam.getTitle(), exam.getCode(), exam.getVisibility(), exam.getAccessMode(),
                new Config(exam.getConfig().getNumberOfQuestions(), exam.getConfig().getTimeLimitMinutes()),
                exam.getMaxAttempts(), exam.getExpiresAt());
    }
}
