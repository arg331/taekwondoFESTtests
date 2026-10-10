package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.NotNull;

public record ChangeExamVisibilityRequest(@NotNull Visibility newVisibility) {}
