package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.result.dto.ResultView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida con la información de un resultado de examen.
 *
 * Incluye el detalle de cada respuesta y el campo derivado "passed"
 * para que el cliente no tenga que conocer el umbral de aprobado.
 */
public record ResultResponse(
        Long id,
        Long examId,
        String studentName,
        String studentClub,
        String studentEmail,
        List<AnswerResponse> answers,
        int correctAnswers,
        int totalQuestions,
        int score,
        boolean passed,
        int timeSpentSeconds,
        LocalDateTime completedAt
) {
    public static ResultResponse from(ResultView view) {
        List<AnswerResponse> answerResponses = view.answers().stream()
                .map(AnswerResponse::from)
                .toList();

        return new ResultResponse(
                view.id(),
                view.examId(),
                view.studentName(),
                view.studentClub(),
                view.studentEmail(),
                answerResponses,
                view.correctAnswers(),
                view.totalQuestions(),
                view.score(),
                view.passed(),
                view.timeSpentSeconds(),
                view.completedAt()
        );
    }
}
