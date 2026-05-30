package com.taekwondo.examenes.application.exam.dto;

/**
 * Datos para cambiar la configuración de un examen.
 *
 * Permite modificar todos los parámetros de comportamiento (tiempo,
 * mostrar nota, randomización...) pero NO la identidad del examen
 * (título, dueño, código) ni su contenido (preguntas).
 *
 * Cambiar el número de preguntas es válido. Si tras el cambio el examen
 * está incompleto, no podrá publicarse hasta completarse, pero seguirá
 * siendo editable.
 */
public record ChangeExamConfigInput(
        Long examId,
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Long requesterOwnerId
) {}