package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Cuerpo para crear un borrador vacío (POST /drafts) o pre-generado con
 * preguntas aleatorias (POST /drafts/pre-generated). En el segundo caso,
 * requiredAnyOfTagIds filtra las preguntas candidatas; vacío = todas.
 */
public record ExamDraftRequest(
        @NotBlank @Size(max = 200) String title,
        @Min(ExamConfig.MIN_QUESTIONS) @Max(ExamConfig.MAX_QUESTIONS) int numberOfQuestions,
        @Min(0) @Max(ExamConfig.MAX_TIME_LIMIT_MINUTES) Integer timeLimitMinutes,
        boolean showScore,
        /** null = lo mismo que showScore; sin nota, nunca se muestran las respuestas. */
        Boolean showAnswers,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Set<Long> requiredAnyOfTagIds
) {}
