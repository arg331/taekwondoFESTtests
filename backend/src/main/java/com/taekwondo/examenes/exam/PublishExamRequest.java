package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * accessMode null = OPEN. expiresAt null = una hora desde ahora.
 */
public record PublishExamRequest(
        @NotNull Visibility visibility,
        ExamAccessMode accessMode,
        LocalDateTime expiresAt
) {}
