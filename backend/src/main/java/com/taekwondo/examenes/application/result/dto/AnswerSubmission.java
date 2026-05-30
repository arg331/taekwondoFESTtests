package com.taekwondo.examenes.application.result.dto;

/**
 * Una respuesta individual enviada por el estudiante.
 *
 * Trae solo la información que el estudiante puede aportar:
 *  - a qué pregunta responde (questionId)
 *  - qué opción ha elegido (chosenOption, 0-3)
 *
 * La respuesta correcta NO viene del cliente: el sistema la consulta
 * del repositorio para evitar manipulaciones desde el frontend.
 */
public record AnswerSubmission(
        Long questionId,
        int chosenOption
) {}
