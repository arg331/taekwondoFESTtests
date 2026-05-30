package com.taekwondo.examenes.application.question.dto;

import com.taekwondo.examenes.domain.model.Difficulty;

import java.util.List;
import java.util.Set;

/**
 * Datos para crear una pregunta nueva.
 *
 * Los tagIds son referencias a tags existentes del profesor.
 * El caso de uso verificará que esos tags existan y pertenezcan al profesor.
 */
public record CreateQuestionInput(
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<Long> tagIds,
        Long ownerId
) {}