package com.taekwondo.examenes.application.question.dto;

import com.taekwondo.examenes.domain.model.Difficulty;

import java.util.List;
import java.util.Set;

/**
 * Datos para editar una pregunta existente.
 *
 * Incluye el contenido editable de la pregunta. Si se modifica algo
 * que aquí no aparece (ej: ownerId), no se cambia.
 *
 * Para asociar/desasociar tags, se envía el conjunto COMPLETO de tags
 * que debe tener la pregunta tras el edit (no un "añadir uno" o "quitar uno").
 * Esto simplifica el contrato y refleja la operación real del usuario:
 * "estos son los tags finales".
 */
public record EditQuestionInput(
        Long questionId,
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<Long> tagIds,
        Long requesterOwnerId
) {}