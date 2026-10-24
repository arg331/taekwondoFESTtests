package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * accessMode null = OPEN. Sin expiresAt, el examen no caduca (RF-48): queda abierto hasta que se cierre.
 * Intentos por alumno (RF-52): maxAttempts de 1 a 4, o unlimitedAttempts; sin ninguno, 1.
 */
public record PublishExamRequest(
        @NotNull Visibility visibility,
        ExamAccessMode accessMode,
        LocalDateTime expiresAt,
        @Min(Exam.MIN_ATTEMPTS) @Max(Exam.MAX_ATTEMPTS) Integer maxAttempts,
        Boolean unlimitedAttempts
) {}
