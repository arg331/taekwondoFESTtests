package com.taekwondo.examenes.question;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/** Cuerpo para crear y para editar una pregunta. */
public record QuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        @NotNull @Size(min = Question.MIN_OPTIONS, max = Question.MAX_OPTIONS,
                message = "debe tener entre " + Question.MIN_OPTIONS + " y " + Question.MAX_OPTIONS + " opciones")
        List<@NotBlank @Size(max = 500) String> options,
        /** Una o varias (RF-09). */
        List<Integer> correctAnswers,
        @Size(max = 2000) String explanation,
        /** Opcional (RF-43). */
        Grade grade,
        QuestionType type,
        Set<Long> tagIds
) {
    @AssertTrue(message = "debe señalar una o más opciones de la pregunta, sin repetir")
    public boolean isCorrectAnswersValid() {
        if (correctAnswers == null || correctAnswers.isEmpty() || correctAnswers.contains(null)) return false;
        boolean inRange = options == null
                || correctAnswers.stream().allMatch(i -> i >= 0 && i < options.size());
        return inRange && Set.copyOf(correctAnswers).size() == correctAnswers.size();
    }
}
