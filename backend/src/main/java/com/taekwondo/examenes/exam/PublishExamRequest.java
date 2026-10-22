package com.taekwondo.examenes.exam;

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
        Boolean neverExpires
) {}
