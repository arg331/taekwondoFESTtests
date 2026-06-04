package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.exam.dto.ExamConfigView;

/**
 * Sub-DTO de ExamResponse para la configuración del examen.
 */
public record ExamConfigResponse(
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {
    public static ExamConfigResponse from(ExamConfigView view) {
        return new ExamConfigResponse(
                view.numberOfQuestions(),
                view.timeLimitMinutes(),
                view.showScore(),
                view.randomizeOptions(),
                view.randomizeQuestionOrder()
        );
    }
}
