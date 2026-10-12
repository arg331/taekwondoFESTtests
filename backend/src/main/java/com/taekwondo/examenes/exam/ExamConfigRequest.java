package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExamConfigRequest(
        @Min(ExamConfig.MIN_QUESTIONS) @Max(ExamConfig.MAX_QUESTIONS) int numberOfQuestions,
        @Min(0) @Max(ExamConfig.MAX_TIME_LIMIT_MINUTES) Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {}
