package com.taekwondo.examenes.dto.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExamConfigRequest(
        @Min(5) @Max(50) int numberOfQuestions,
        @Min(0) @Max(180) Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {}
