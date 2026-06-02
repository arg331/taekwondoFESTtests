package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * Body de PATCH /api/exams/{id}/config.
 *
 * Envío completo (no PATCH parcial): el cliente envía siempre todos
 * los campos. Más simple y predecible que un patch tipo JSON Merge.
 */
public record ChangeExamConfigRequest(
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {}
