package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.question.dto.QuestionView;

import java.util.List;

/**
 * DTO público de pregunta para el flujo exam-take.
 *
 * No expone correctAnswer ni explanation — el estudiante
 * no debe ver la respuesta antes de enviar el examen.
 */
public record PublicQuestionResponse(
        Long id,
        String text,
        List<String> options
) {
    public static PublicQuestionResponse from(QuestionView view) {
        return new PublicQuestionResponse(
                view.id(),
                view.text(),
                view.options()
        );
    }
}
