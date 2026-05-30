package com.taekwondo.examenes.application.result.dto;

/**
 * Estadísticas agregadas de un examen.
 *
 * - totalAttempts: número de intentos completados.
 * - averageScore: nota media (0.0 - 100.0).
 * - passedCount: cuántos estudiantes superaron el umbral.
 * - failedCount: cuántos no lo superaron.
 * - highestScore / lowestScore: mejor y peor nota.
 *
 * Si no hay intentos, averageScore es 0 y highest/lowest son 0.
 * El cliente puede decidir cómo mostrar "sin datos".
 */
public record ExamStatisticsView(
        Long examId,
        int totalAttempts,
        double averageScore,
        int passedCount,
        int failedCount,
        int highestScore,
        int lowestScore
) {}
