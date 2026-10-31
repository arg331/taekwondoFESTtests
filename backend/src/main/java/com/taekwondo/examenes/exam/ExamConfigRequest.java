package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExamConfigRequest(
        @Min(ExamConfig.MIN_QUESTIONS) @Max(ExamConfig.MAX_QUESTIONS) int numberOfQuestions,
        @Min(0) @Max(ExamConfig.MAX_TIME_LIMIT_MINUTES) Integer timeLimitMinutes,
        boolean showScore,
        /** null = lo mismo que showScore; sin nota, nunca se muestran las respuestas. */
        Boolean showAnswers,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder,
        /** % de aciertos para aprobar; null = el 50 % (DE-13). */
        @Min(ExamConfig.MIN_PASSING_SCORE) @Max(ExamConfig.MAX_PASSING_SCORE) Integer passingScore
) {}
