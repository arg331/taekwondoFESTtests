package com.taekwondo.examenes.application.question.dto;

import java.util.Set;

/**
 * Filtros de búsqueda de preguntas.
 *
 * Todos los campos excepto ownerId son opcionales. El ownerId es siempre
 * obligatorio: cada profesor solo busca en sus propias preguntas.
 *
 * Si tagIds está vacío o es null, no se filtra por tags.
 * Si textContains está vacío o es null, no se filtra por texto.
 */
public record SearchQuestionsInput(
        Long ownerId,
        Set<Long> anyOfTagIds,
        String textContains
) {}