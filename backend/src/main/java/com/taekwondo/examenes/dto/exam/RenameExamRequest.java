package com.taekwondo.examenes.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameExamRequest(@NotBlank @Size(max = 200) String newTitle) {}
