package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.result.dto.ExamStatisticsView;

/**
 * DTO de salida con estadísticas agregadas de un examen.
 */
public record ExamStatisticsResponse(
        Long examId,
        int totalAttempts,
        double averageScore,
        int passedCount,
        int failedCount,
        int highestScore,
        int lowestScore
) {
    public static ExamStatisticsResponse from(ExamStatisticsView view) {
        return new ExamStatisticsResponse(
                view.examId(),
                view.totalAttempts(),
                view.averageScore(),
                view.passedCount(),
                view.failedCount(),
                view.highestScore(),
                view.lowestScore()
        );
    }
}
