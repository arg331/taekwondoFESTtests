package com.taekwondo.examenes.result;

public record ExamStatisticsResponse(
        Long examId,
        int totalAttempts,
        double averageScore,
        int passedCount,
        int failedCount,
        int highestScore,
        int lowestScore
) {}
