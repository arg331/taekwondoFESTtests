package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.domain.model.Visibility;

import java.time.LocalDateTime;

/**
 * Body de POST /api/exams/{id}/publish.
 *
 * expiresAt es opcional. Si es null, el caso de uso aplica el default
 * (+1 hora desde la publicación).
 */
public record PublishExamRequest(
        Visibility visibility,
        LocalDateTime expiresAt
) {}
