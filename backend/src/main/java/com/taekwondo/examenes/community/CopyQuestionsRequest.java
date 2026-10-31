package com.taekwondo.examenes.community;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Las preguntas del examen que se copian a mi banco (DE-12). */
public record CopyQuestionsRequest(
        @NotEmpty(message = "elige al menos una pregunta") List<@NotNull Long> questionIds
) {}
