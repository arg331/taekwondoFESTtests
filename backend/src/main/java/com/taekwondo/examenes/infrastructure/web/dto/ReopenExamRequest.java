package com.taekwondo.examenes.infrastructure.web.dto;

import java.time.LocalDateTime;

/**
 * Body de POST /api/exams/{id}/reopen.
 *
 * newExpiresAt puede ser null (sin límite temporal).
 * El caso de uso valida que, si se proporciona, sea futura.
 *
 * El código original se mantiene: el QR antiguo sigue siendo válido.
 */
public record ReopenExamRequest(
        LocalDateTime newExpiresAt
) {}
