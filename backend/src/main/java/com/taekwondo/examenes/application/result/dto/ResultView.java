package com.taekwondo.examenes.application.result.dto;

import com.taekwondo.examenes.domain.model.Result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vista de un resultado para devolver desde la capa de aplicación.
 *
 * Reutilizada en submit, get, list. La conversión Result -> ResultView
 * vive en el método from() para centralizar el mapeo.
 *
 * Incluye un campo derivado "passed" (boolean) para que el cliente
 * no tenga que conocer el umbral de aprobado (lo decide el dominio).
 */
public record ResultView(
        Long id,
        Long examId,
        String studentName,
        String studentClub,
        String studentEmail,
        List<AnswerView> answers,
        int correctAnswers,
        int totalQuestions,
        int score,
        boolean passed,
        int timeSpentSeconds,
        LocalDateTime completedAt
) {
    public static ResultView from(Result result) {
        List<AnswerView> answerViews = result.getAnswers().stream()
                .map(AnswerView::from)
                .collect(Collectors.toList());

        return new ResultView(
                result.getId(),
                result.getExamId(),
                result.getStudentName(),
                result.getStudentClub(),
                result.getStudentEmail(),
                answerViews,
                result.getCorrectAnswers(),
                result.getTotalQuestions(),
                result.getScore(),
                result.isPassed(),
                result.getTimeSpentSeconds(),
                result.getCompletedAt()
        );
    }
}
