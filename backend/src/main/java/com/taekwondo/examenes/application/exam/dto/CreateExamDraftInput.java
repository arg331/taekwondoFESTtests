package com.taekwondo.examenes.application.exam.dto;

/**
 * Datos para crear un examen draft VACÍO (sin pre-generación).
 *
 * Modo manual: el profesor configura el examen y luego añade preguntas
 * a mano en la vista de ajuste.
 */
public record CreateExamDraftInput(
        String title,
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        Long ownerId
) {}