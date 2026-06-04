package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object embebido para persistir Answer dentro de la tabla
 * result_answers (via @ElementCollection en ResultJpaEntity).
 *
 * Cada fila representa una respuesta del estudiante a una pregunta
 * concreta en un resultado concreto. Es snapshot histórico inmutable:
 * guarda tanto la opción elegida como la correcta en ese momento.
 *
 * isCorrect() del dominio no se persiste: se deriva en el mapper.
 */
@Embeddable
public class AnswerJpaEmbeddable {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "student_answer", nullable = false)
    private int studentAnswer;

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    public AnswerJpaEmbeddable() {
        // Requerido por JPA
    }

    public AnswerJpaEmbeddable(Long questionId, int studentAnswer, int correctAnswer) {
        this.questionId = questionId;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
    }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public int getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(int studentAnswer) { this.studentAnswer = studentAnswer; }

    public int getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(int correctAnswer) { this.correctAnswer = correctAnswer; }
}
