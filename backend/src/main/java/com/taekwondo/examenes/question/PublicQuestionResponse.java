package com.taekwondo.examenes.question;

import java.util.List;

/**
 * Pregunta tal como la ve el alumno durante el examen: sin respuesta correcta
 * ni explicación. Cada opción lleva su índice original, que es el que el
 * alumno envía al entregar (así las opciones se pueden mostrar barajadas).
 */
public record PublicQuestionResponse(Long id, String text, List<Option> options,
                                     /** Tiene varias correctas: hay que marcarlas todas (RF-09). */
                                     boolean multipleCorrect) {

    public record Option(int index, String text) {}
}
