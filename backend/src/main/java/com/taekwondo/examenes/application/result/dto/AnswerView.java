package com.taekwondo.examenes.application.result.dto;

import com.taekwondo.examenes.domain.model.Answer;

/**
 * Vista de una respuesta individual.
 *
 * Incluye toda la información para que el cliente pueda mostrar el detalle:
 * qué pregunta, qué eligió el estudiante, cuál era correcta, y si acertó.
 */
public record AnswerView(
        Long questionId,
        int studentAnswer,
        int correctAnswer,
        boolean correct
) {
    public static AnswerView from(Answer answer) {
        return new AnswerView(
                answer.getQuestionId(),
                answer.getStudentAnswer(),
                answer.getCorrectAnswer(),
                answer.isCorrect()
        );
    }
}
