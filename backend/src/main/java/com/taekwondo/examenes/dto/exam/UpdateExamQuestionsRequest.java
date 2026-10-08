package com.taekwondo.examenes.dto.exam;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateExamQuestionsRequest(@NotNull List<@NotNull Long> questionIds) {}
