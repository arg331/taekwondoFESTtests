package com.taekwondo.examenes.infrastructure.web.dto;

import java.util.Set;

/**
 * Body de POST /api/exams/drafts/pre-generated.
 *
 * requiredAnyOfTagIds: tags filtrados (OR). Las preguntas seleccionadas
 * deben tener al menos uno de estos tags. Si vacío o null, se eligen
 * aleatoriamente entre todas las preguntas del profesor.
 */
public record PreGenerateExamDraftRequest(
        String title,
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Set<Long> requiredAnyOfTagIds
) {}
