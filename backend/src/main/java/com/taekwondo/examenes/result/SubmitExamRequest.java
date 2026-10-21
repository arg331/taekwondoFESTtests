package com.taekwondo.examenes.result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitExamRequest(
        @NotBlank String examCode,
        @NotBlank String attemptToken,
        @NotNull List<@NotNull @Valid Answer> answers
) {
    /**
     * Opciones marcadas; vacío o null = pregunta sin responder. Que existan en la pregunta lo comprueba
     * la corrección. Puede haber varias (RF-09).
     */
    public record Answer(@NotNull Long questionId, List<@NotNull @Min(0) Integer> chosenOptions) {}
}
