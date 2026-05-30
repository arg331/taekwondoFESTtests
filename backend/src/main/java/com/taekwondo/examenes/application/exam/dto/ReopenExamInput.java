package com.taekwondo.examenes.application.exam.dto;

import java.time.LocalDateTime;

/**
 * Datos para reabrir un examen previamente expirado (manual o por tiempo).
 *
 * newExpiresAt: nueva fecha de expiración. Puede ser null (sin expiración
 * por tiempo). El caso de uso validará que sea futura si se proporciona.
 *
 * Decisión de diseño: el examen mantiene el MISMO código (el QR antiguo
 * sigue funcionando). Esto fue acordado: simplifica el flujo del profesor
 * cuando falta un estudiante por hacer un examen ya cerrado.
 */
public record ReopenExamInput(
        Long examId,
        LocalDateTime newExpiresAt,
        Long requesterOwnerId
) {}