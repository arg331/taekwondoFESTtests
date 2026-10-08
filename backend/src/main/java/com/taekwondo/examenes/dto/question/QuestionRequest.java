package com.taekwondo.examenes.dto.question;

import com.taekwondo.examenes.entity.Difficulty;
import com.taekwondo.examenes.entity.Question;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/** Cuerpo para crear y para editar una pregunta. */
public record QuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        @NotNull @Size(min = Question.MIN_OPTIONS, max = Question.MAX_OPTIONS,
                message = "debe tener entre 2 y 4 opciones")
        List<@NotBlank @Size(max = 500) String> options,
        @Min(0) int correctAnswer,
        @Size(max = 2000) String explanation,
        @NotNull Difficulty difficulty,
        Set<Long> tagIds
) {
    @AssertTrue(message = "debe señalar una de las opciones")
    public boolean isCorrectAnswerInRange() {
        return options == null || correctAnswer < options.size();
    }
}
