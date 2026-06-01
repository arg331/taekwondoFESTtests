package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.domain.model.Difficulty;

import java.util.List;
import java.util.Set;

/**
 * DTO HTTP de entrada para crear una pregunta.
 *
 * El ownerId NO viene aquí: el controller lo obtiene del usuario
 * autenticado. El cliente no debe poder elegir el dueño de las
 * preguntas que crea.
 *
 * Los tagIds son referencias a tags existentes del profesor. El caso
 * de uso verifica que existan y que pertenezcan al ownerId.
 */
public record CreateQuestionRequest(
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<Long> tagIds
) {}
