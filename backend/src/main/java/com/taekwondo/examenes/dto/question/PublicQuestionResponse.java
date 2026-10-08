package com.taekwondo.examenes.dto.question;

import java.util.List;

/**
 * Pregunta tal como la ve el alumno durante el examen: sin respuesta correcta
 * ni explicación. Cada opción lleva su índice original, que es el que el
 * alumno envía al entregar (así las opciones se pueden mostrar barajadas).
 */
public record PublicQuestionResponse(Long id, String text, List<Option> options) {

    public record Option(int index, String text) {}
}
