package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.domain.model.Difficulty;

import java.util.List;
import java.util.Set;

/**
 * DTO HTTP de entrada para editar una pregunta existente.
 *
 * El questionId NO viene aquí: viaja en la URL (PATCH /api/questions/{id}).
 * El requesterOwnerId tampoco: lo añade el controller desde el usuario
 * autenticado.
 *
 * Los tagIds son el conjunto COMPLETO de tags que tendrá la pregunta
 * tras el edit (no un "añadir" o "quitar" individual).
 */
public record EditQuestionRequest(
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<Long> tagIds
) {}
