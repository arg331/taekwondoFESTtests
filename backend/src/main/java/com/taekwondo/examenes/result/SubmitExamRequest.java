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
    /** chosenOption null = pregunta sin responder; que exista en la pregunta lo comprueba la corrección. */
    public record Answer(@NotNull Long questionId, @Min(0) Integer chosenOption) {}
}
