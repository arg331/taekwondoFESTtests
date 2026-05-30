package com.taekwondo.examenes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para configurar la creación de un nuevo examen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamConfigDTO {

    @NotNull(message = "El número de preguntas es obligatorio")
    @Min(value = 5, message = "Mínimo 5 preguntas")
    @Max(value = 50, message = "Máximo 50 preguntas")
    private Integer numberOfQuestions;

    @NotNull(message = "Debes especificar si mostrar la nota")
    private Boolean showScore;

    @NotNull(message = "El tiempo límite es obligatorio (0 = sin límite)")
    @Min(value = 0, message = "El tiempo no puede ser negativo")
    @Max(value = 180, message = "Máximo 180 minutos")
    private Integer timeLimit; // en minutos, 0 = sin límite

    @NotNull(message = "Debes especificar si aleatorizar las opciones")
    private Boolean randomizeOptions;
}
