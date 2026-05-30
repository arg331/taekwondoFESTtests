package com.taekwondo.examenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para enviar las respuestas de un examen completado
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmissionDTO {

    @NotBlank(message = "El nombre del estudiante es obligatorio")
    private String studentName;

    private String studentClub; // Opcional

    private String studentEmail; // Opcional

    @NotNull(message = "Las respuestas son obligatorias")
    private List<AnswerDTO> answers;

    @NotNull(message = "El tiempo empleado es obligatorio")
    private Integer timeSpent; // en segundos

    /**
     * DTO interno para cada respuesta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDTO {
        
        @NotNull(message = "El ID de la pregunta es obligatorio")
        private Long questionId;

        @NotNull(message = "La respuesta del estudiante es obligatoria")
        private Integer studentAnswer; // índice 0-3
    }
}
