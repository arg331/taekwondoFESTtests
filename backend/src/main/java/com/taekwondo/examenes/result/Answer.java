package com.taekwondo.examenes.result;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Respuesta de un alumno a una pregunta. Guarda también las respuestas correctas
 * en el momento de la entrega, para que el resultado no cambie si el profesor
 * edita la pregunta después. Una pregunta puede tener varias correctas (RF-09):
 * se acierta solo marcando exactamente esas.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Opciones que marcó el alumno, de menor a mayor; vacío si la dejó en blanco. */
    @Convert(converter = OptionListConverter.class)
    @Column(name = "student_options", nullable = false, length = 100)
    private List<Integer> studentAnswers;

    @Convert(converter = OptionListConverter.class)
    @Column(name = "correct_options", nullable = false, length = 100)
    private List<Integer> correctAnswers;

    public Answer(Long questionId, List<Integer> studentAnswers, List<Integer> correctAnswers) {
        this.questionId = questionId;
        this.studentAnswers = studentAnswers.stream().distinct().sorted().toList();
        this.correctAnswers = correctAnswers.stream().distinct().sorted().toList();
    }

    public boolean isCorrect() {
        return !studentAnswers.isEmpty() && Set.copyOf(studentAnswers).equals(Set.copyOf(correctAnswers));
    }
}
