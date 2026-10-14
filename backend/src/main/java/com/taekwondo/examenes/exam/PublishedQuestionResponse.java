package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.question.Question;

import java.util.List;

/**
 * Pregunta tal como se publicó en un examen (DE-03), para su profesor. editedSincePublished
 * avisa de que la del banco ya no coincide (o ya no existe).
 */
public record PublishedQuestionResponse(
        Long questionId,
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        boolean editedSincePublished
) {
    static PublishedQuestionResponse from(PublishedQuestion published, Question current) {
        return new PublishedQuestionResponse(published.getQuestionId(), published.getText(),
                List.copyOf(published.getOptions()), published.getCorrectAnswer(), published.getExplanation(),
                current == null || !published.matches(current));
    }
}
