package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.result.dto.AnswerView;

/**
 * Sub-DTO dentro de ResultResponse.
 * Detalle de una respuesta individual con resultado.
 */
public record AnswerResponse(
        Long questionId,
        int studentAnswer,
        int correctAnswer,
        boolean correct
) {
    public static AnswerResponse from(AnswerView view) {
        return new AnswerResponse(
                view.questionId(),
                view.studentAnswer(),
                view.correctAnswer(),
                view.correct()
        );
    }
}
