package com.taekwondo.examenes.application.exam.dto;

import java.util.List;

/**
 * Datos para actualizar la lista completa de preguntas de un examen.
 *
 * El orden importa: define el orden en que aparecerán al estudiante
 * (si randomizeQuestionOrder está desactivado).
 *
 * Sustituye TODA la lista actual por la nueva. Equivale a:
 *   - quitar todas las preguntas
 *   - añadir las nuevas en orden
 *
 * Esto refleja la operación real del frontend: el usuario arrastra y
 * deja preguntas, y al guardar manda la lista completa final.
 */
public record UpdateExamQuestionsInput(
        Long examId,
        List<Long> questionIds,
        Long requesterOwnerId
) {}