package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * Body de POST /api/exams/drafts.
 *
 * No incluye ownerId: lo obtenemos del JWT en el controller.
 */
public record CreateExamDraftRequest(
        String title,
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {}
