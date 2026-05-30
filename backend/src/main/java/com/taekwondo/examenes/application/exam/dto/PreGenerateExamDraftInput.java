package com.taekwondo.examenes.application.exam.dto;

import java.util.Set;

/**
 * Datos para pre-generar un draft de examen con preguntas aleatorias
 * filtradas por tags obligatorios.
 *
 * Tags obligatorios: las preguntas seleccionadas deben tener AL MENOS
 * UNO de estos tags (OR). Si no hay tags, se eligen aleatoriamente
 * entre todas las preguntas del profesor.
 */
public record PreGenerateExamDraftInput(
        String title,
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Set<Long> requiredAnyOfTagIds,
        Long ownerId
) {}