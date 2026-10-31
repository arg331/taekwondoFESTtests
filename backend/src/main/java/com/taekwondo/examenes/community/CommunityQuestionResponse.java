package com.taekwondo.examenes.community;

import com.taekwondo.examenes.exam.PublishedQuestion;

import java.util.List;

/** Pregunta de un examen de prueba ajeno tal como se publicó, con su solución, para decidir si copiarla (DE-12). */
public record CommunityQuestionResponse(
        Long questionId,
        String text,
        List<String> options,
        List<Integer> correctAnswers,
        String explanation
) {
    static CommunityQuestionResponse from(PublishedQuestion published) {
        return new CommunityQuestionResponse(published.getQuestionId(), published.getText(),
                List.copyOf(published.getOptions()), published.getCorrectAnswers(), published.getExplanation());
    }
}
