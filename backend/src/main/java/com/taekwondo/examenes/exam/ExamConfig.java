package com.taekwondo.examenes.exam;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Parámetros de cómo se presenta y corrige un examen.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamConfig {

    public static final int MIN_QUESTIONS = 5;
    public static final int MAX_QUESTIONS = 50;
    public static final int MAX_TIME_LIMIT_MINUTES = 180;

    /**
     * Preguntas que pide el examen. En un borrador es el objetivo (para generar al azar o
     * completar); al publicar pasa a ser el número de preguntas elegidas.
     */
    @Column(name = "number_of_questions", nullable = false)
    private int numberOfQuestions;

    /** null o 0 = sin límite. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "show_score", nullable = false)
    private boolean showScore;

    /** Además de la nota, el alumno ve sus respuestas bien o mal, nunca la correcta (RF-53). Requiere showScore. */
    @Column(name = "show_answers", nullable = false)
    private boolean showAnswers;

    @Column(name = "randomize_options", nullable = false)
    private boolean randomizeOptions;

    @Column(name = "randomize_question_order", nullable = false)
    private boolean randomizeQuestionOrder;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    void matchQuestionCount(int count) {
        numberOfQuestions = count;
    }
}
