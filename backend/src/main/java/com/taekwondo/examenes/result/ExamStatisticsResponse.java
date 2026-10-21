package com.taekwondo.examenes.result;

/**
 * Estadísticas de un examen (RF-26, RF-54): totalAttempts cuenta las entregas; el resto, los alumnos
 * (totalStudents) con su mejor nota.
 */
public record ExamStatisticsResponse(
        Long examId,
        int totalAttempts,
        int totalStudents,
        double averageScore,
        int passedCount,
        int failedCount,
        int highestScore,
        int lowestScore
) {}
