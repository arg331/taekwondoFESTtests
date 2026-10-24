package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Cambiar los intentos por alumno de un examen publicado (RF-52): de 1 a 4, o sin límite. */
public record ChangeAttemptsRequest(
        @Min(Exam.MIN_ATTEMPTS) @Max(Exam.MAX_ATTEMPTS) Integer maxAttempts,
        Boolean unlimitedAttempts
) {}
