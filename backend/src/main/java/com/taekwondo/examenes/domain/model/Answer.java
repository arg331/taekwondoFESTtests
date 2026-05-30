package com.taekwondo.examenes.domain.model;

import java.util.Objects;

/**
 * Value object: respuesta de un estudiante a una pregunta concreta
 * dentro de un examen.
 *
 * Es un value object (no tiene identidad propia) que vive dentro de un
 * Result. Almacena tanto la respuesta del estudiante como la respuesta
 * correcta TAL Y COMO ERA EN EL MOMENTO del examen.
 *
 * Esto es deliberado: si más tarde el profesor edita la pregunta y
 * cambia la opción correcta, el Answer antiguo sigue representando
 * fielmente lo que pasó (log histórico inmutable).
 *
 * Inmutable: una vez creado, no cambia.
 */
public final class Answer {

    private static final int MIN_OPTION_INDEX = 0;
    private static final int MAX_OPTION_INDEX = 3;

    private final Long questionId;
    private final int studentAnswer;
    private final int correctAnswer;

    public static Answer of(Long questionId, int studentAnswer, int correctAnswer) {
        validateQuestionId(questionId);
        validateOptionIndex(studentAnswer, "respuesta del estudiante");
        validateOptionIndex(correctAnswer, "respuesta correcta");
        return new Answer(questionId, studentAnswer, correctAnswer);
    }

    private Answer(Long questionId, int studentAnswer, int correctAnswer) {
        this.questionId = questionId;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
    }

    /**
     * Indica si la respuesta del estudiante fue correcta.
     * Es información derivada (no se persiste como campo).
     */
    public boolean isCorrect() {
        return studentAnswer == correctAnswer;
    }

    private static void validateQuestionId(Long questionId) {
        Objects.requireNonNull(questionId, "questionId no puede ser null");
    }

    private static void validateOptionIndex(int index, String label) {
        if (index < MIN_OPTION_INDEX || index > MAX_OPTION_INDEX) {
            throw new IllegalArgumentException(
                    label + " debe estar entre " + MIN_OPTION_INDEX
                            + " y " + MAX_OPTION_INDEX);
        }
    }

    public Long getQuestionId()     { return questionId; }
    public int getStudentAnswer()   { return studentAnswer; }
    public int getCorrectAnswer()   { return correctAnswer; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Answer a)) return false;
        return studentAnswer == a.studentAnswer
                && correctAnswer == a.correctAnswer
                && Objects.equals(questionId, a.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, studentAnswer, correctAnswer);
    }
}
