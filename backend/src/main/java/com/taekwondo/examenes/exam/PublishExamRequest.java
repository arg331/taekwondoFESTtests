package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * accessMode null = OPEN. Sin expiresAt, el examen caduca en una hora,
 * salvo que neverExpires sea true: entonces no caduca nunca.
 */
public record PublishExamRequest(
        @NotNull Visibility visibility,
        ExamAccessMode accessMode,
        LocalDateTime expiresAt,
        Boolean neverExpires,
        /** Intentos por alumno (RF-52); null = 1. */
        @Min(Exam.MIN_ATTEMPTS) @Max(Exam.MAX_ATTEMPTS) Integer maxAttempts
) {}
