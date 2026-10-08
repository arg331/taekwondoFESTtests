package com.taekwondo.examenes.dto.exam;

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
        @Min(5) @Max(50) int numberOfQuestions,
        @Min(0) @Max(180) Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Set<Long> requiredAnyOfTagIds
) {}
