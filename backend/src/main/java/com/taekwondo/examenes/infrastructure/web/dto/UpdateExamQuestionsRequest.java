package com.taekwondo.examenes.infrastructure.web.dto;

import java.util.List;

/**
 * Body de PUT /api/exams/{id}/questions.
 *
 * El cliente envía siempre la lista COMPLETA y final de preguntas.
 * El orden importa: define el orden de visualización en el examen
 * (si randomizeQuestionOrder está desactivado).
 */
public record UpdateExamQuestionsRequest(
        List<Long> questionIds
) {}
