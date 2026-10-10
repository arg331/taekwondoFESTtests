package com.taekwondo.examenes.result;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta de un alumno a una pregunta. Guarda también la respuesta correcta
 * en el momento de la entrega, para que el resultado no cambie si el profesor
 * edita la pregunta después.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Answer {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Índice elegido por el alumno, o null si la dejó en blanco. */
    @Column(name = "student_answer")
    private Integer studentAnswer;

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    public boolean isCorrect() {
        return studentAnswer != null && studentAnswer == correctAnswer;
    }
}
