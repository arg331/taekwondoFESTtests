package com.taekwondo.examenes.dto.exam;

import com.taekwondo.examenes.entity.Visibility;
import jakarta.validation.constraints.NotNull;

public record ChangeExamVisibilityRequest(@NotNull Visibility newVisibility) {}
