package com.taekwondo.examenes.infrastructure.web.dto;

import java.time.LocalDateTime;

/**
 * Body de PATCH /api/exams/{id}/expiration.
 *
 * newExpiresAt puede ser null (sin límite temporal).
 * El caso de uso valida que, si se proporciona, sea futura.
 */
public record ExtendExamExpirationRequest(
        LocalDateTime newExpiresAt
) {}
