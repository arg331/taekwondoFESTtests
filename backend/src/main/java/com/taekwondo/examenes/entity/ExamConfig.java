package com.taekwondo.examenes.entity;

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

    @Column(name = "number_of_questions", nullable = false)
    private int numberOfQuestions;

    /** null o 0 = sin límite. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "show_score", nullable = false)
    private boolean showScore;

    @Column(name = "randomize_options", nullable = false)
    private boolean randomizeOptions;

    @Column(name = "randomize_question_order", nullable = false)
    private boolean randomizeQuestionOrder;

    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }
}
