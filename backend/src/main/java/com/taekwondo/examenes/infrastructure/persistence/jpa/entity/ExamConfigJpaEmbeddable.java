package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Value object embebido para persistir ExamConfig dentro de la tabla exams.
 *
 * Al ser @Embeddable, sus campos se "aplanan" como columnas adicionales
 * de la entidad propietaria (ExamJpaEntity). Sin tabla aparte.
 *
 * Esta clase es PUR JPA: el mapper convierte entre el value object
 * inmutable del dominio (ExamConfig) y esta representación mutable
 * que JPA puede manejar.
 */
@Embeddable
public class ExamConfigJpaEmbeddable {

    @Column(name = "number_of_questions", nullable = false)
    private int numberOfQuestions;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;   // nullable: sin límite

    @Column(name = "show_score", nullable = false)
    private boolean showScore;

    @Column(name = "randomize_options", nullable = false)
    private boolean randomizeOptions;

    @Column(name = "randomize_question_order", nullable = false)
    private boolean randomizeQuestionOrder;

    public ExamConfigJpaEmbeddable() {
        // Requerido por JPA
    }

    public ExamConfigJpaEmbeddable(int numberOfQuestions,
                                    Integer timeLimitMinutes,
                                    boolean showScore,
                                    boolean randomizeOptions,
                                    boolean randomizeQuestionOrder) {
        this.numberOfQuestions = numberOfQuestions;
        this.timeLimitMinutes = timeLimitMinutes;
        this.showScore = showScore;
        this.randomizeOptions = randomizeOptions;
        this.randomizeQuestionOrder = randomizeQuestionOrder;
    }

    public int getNumberOfQuestions() { return numberOfQuestions; }
    public void setNumberOfQuestions(int numberOfQuestions) { this.numberOfQuestions = numberOfQuestions; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public boolean isShowScore() { return showScore; }
    public void setShowScore(boolean showScore) { this.showScore = showScore; }

    public boolean isRandomizeOptions() { return randomizeOptions; }
    public void setRandomizeOptions(boolean randomizeOptions) { this.randomizeOptions = randomizeOptions; }

    public boolean isRandomizeQuestionOrder() { return randomizeQuestionOrder; }
    public void setRandomizeQuestionOrder(boolean randomizeQuestionOrder) { this.randomizeQuestionOrder = randomizeQuestionOrder; }
}
