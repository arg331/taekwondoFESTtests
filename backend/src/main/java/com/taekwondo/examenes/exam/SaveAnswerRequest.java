package com.taekwondo.examenes.exam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Lo marcado en una pregunta, guardado al contestarla (DE-05). Sustituye a lo anterior; vacío o null
 * = sin responder. Que las opciones existan lo comprueba el intento.
 */
public record SaveAnswerRequest(
        @NotBlank String attemptToken,
        @NotNull Long questionId,
        List<@NotNull @Min(0) Integer> chosenOptions
) {}
