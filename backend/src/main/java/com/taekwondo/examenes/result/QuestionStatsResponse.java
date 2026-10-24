package com.taekwondo.examenes.result;

import com.taekwondo.examenes.exam.PublishedQuestion;

import java.util.Arrays;
import java.util.List;

/**
 * Cómo se ha respondido una pregunta en las entregas de un examen (RF-28): cuántas la acertaron,
 * cuántas la dejaron en blanco y cuántas marcaron cada opción, para ver si alguna confunde.
 * successRate es el % de acierto redondeado; null si nadie ha entregado todavía.
 */
public record QuestionStatsResponse(
        Long questionId,
        String text,
        List<String> options,
        List<Integer> correctAnswers,
        int submissions,
        int correct,
        int blank,
        /** optionCounts.get(i): entregas que marcaron la opción i. */
        List<Integer> optionCounts,
        Integer successRate
) {
    static QuestionStatsResponse of(PublishedQuestion question, List<Answer> answers) {
        int[] counts = new int[question.getOptions().size()];
        answers.forEach(a -> a.getStudentAnswers().stream()
                .filter(option -> option < counts.length)
                .forEach(option -> counts[option]++));
        int correct = (int) answers.stream().filter(Answer::isCorrect).count();
        int blank = (int) answers.stream().filter(a -> a.getStudentAnswers().isEmpty()).count();
        Integer successRate = answers.isEmpty() ? null : Math.round(correct * 100f / answers.size());
        return new QuestionStatsResponse(question.getQuestionId(), question.getText(), List.copyOf(question.getOptions()),
                question.getCorrectAnswers(), answers.size(), correct, blank, Arrays.stream(counts).boxed().toList(),
                successRate);
    }
}
