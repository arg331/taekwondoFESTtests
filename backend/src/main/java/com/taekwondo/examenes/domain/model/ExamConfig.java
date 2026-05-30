package com.taekwondo.examenes.domain.model;

import java.util.Objects;

/**
 * Value object: configuración de comportamiento de un examen.
 *
 * Agrupa los parámetros que afectan a cómo se presenta y corrige el examen,
 * separándolos de la identidad del examen en sí (SRP).
 *
 * Inmutable. Si cambia algún parámetro, se crea una nueva instancia con
 * los métodos withXxx().
 */
public final class ExamConfig {

    private static final int MIN_QUESTIONS = 5;
    private static final int MAX_QUESTIONS = 50;
    private static final int MAX_TIME_LIMIT_MINUTES = 180;

    private final int numberOfQuestions;
    private final Integer timeLimitMinutes;   // null = sin límite
    private final boolean showScore;
    private final boolean randomizeOptions;
    private final boolean randomizeQuestionOrder;

    public static ExamConfig of(int numberOfQuestions,
                                 Integer timeLimitMinutes,
                                 boolean showScore,
                                 boolean randomizeOptions,
                                 boolean randomizeQuestionOrder) {
        validateNumberOfQuestions(numberOfQuestions);
        validateTimeLimit(timeLimitMinutes);
        return new ExamConfig(numberOfQuestions, timeLimitMinutes,
                showScore, randomizeOptions, randomizeQuestionOrder);
    }

    private ExamConfig(int numberOfQuestions,
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

    private static void validateNumberOfQuestions(int n) {
        if (n < MIN_QUESTIONS || n > MAX_QUESTIONS) {
            throw new IllegalArgumentException(
                    "El número de preguntas debe estar entre " + MIN_QUESTIONS
                            + " y " + MAX_QUESTIONS);
        }
    }

    private static void validateTimeLimit(Integer minutes) {
        if (minutes == null) return; // sin límite es válido
        if (minutes < 0 || minutes > MAX_TIME_LIMIT_MINUTES) {
            throw new IllegalArgumentException(
                    "El tiempo límite debe estar entre 0 y "
                            + MAX_TIME_LIMIT_MINUTES + " minutos");
        }
    }

    public int getNumberOfQuestions()        { return numberOfQuestions; }
    public Integer getTimeLimitMinutes()     { return timeLimitMinutes; }
    public boolean isShowScore()             { return showScore; }
    public boolean isRandomizeOptions()      { return randomizeOptions; }
    public boolean isRandomizeQuestionOrder(){ return randomizeQuestionOrder; }
    public boolean hasTimeLimit()            { return timeLimitMinutes != null && timeLimitMinutes > 0; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamConfig c)) return false;
        return numberOfQuestions == c.numberOfQuestions
                && showScore == c.showScore
                && randomizeOptions == c.randomizeOptions
                && randomizeQuestionOrder == c.randomizeQuestionOrder
                && Objects.equals(timeLimitMinutes, c.timeLimitMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfQuestions, timeLimitMinutes,
                showScore, randomizeOptions, randomizeQuestionOrder);
    }
}