package com.taekwondo.examenes.dto.result;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitExamRequest(
        @NotBlank String examCode,
        @NotBlank String attemptToken,
        @NotBlank @Size(max = 100) String studentName,
        @Size(max = 100) String studentClub,
        @Email @Size(max = 200) String studentEmail,
        @NotNull List<@NotNull @Valid Answer> answers
) {
    /** chosenOption null = pregunta sin responder. */
    public record Answer(@NotNull Long questionId, @Min(0) @Max(3) Integer chosenOption) {}
}
