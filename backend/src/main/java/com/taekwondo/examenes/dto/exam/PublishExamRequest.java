package com.taekwondo.examenes.dto.exam;

import com.taekwondo.examenes.entity.ExamAccessMode;
import com.taekwondo.examenes.entity.Visibility;
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
