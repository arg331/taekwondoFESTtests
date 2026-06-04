package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * Sub-DTO dentro de SubmitExamRequest.
 *
 * Solo lleva información que el estudiante puede aportar:
 * - questionId: a qué pregunta responde
 * - chosenOption: qué opción ha elegido (0-3)
 *
 * La respuesta correcta NO viene del cliente: el servidor la consulta
 * para evitar manipulaciones.
 */
public record AnswerSubmissionRequest(
        Long questionId,
        int chosenOption
) {}
