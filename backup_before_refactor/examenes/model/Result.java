package com.taekwondo.examenes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa el resultado de un examen realizado
 */
@Entity
@Table(name = "results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia al examen realizado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /**
     * Nombre del estudiante
     */
    @Column(nullable = false)
    private String studentName;

    /**
     * Club/Academia (opcional)
     */
    @Column
    private String studentClub;

    /**
     * Email del estudiante (opcional)
     */
    @Column
    private String studentEmail;

    /**
     * Puntuación obtenida (porcentaje 0-100)
     */
    @Column(nullable = false)
    private Integer score;

    /**
     * Número de respuestas correctas
     */
    @Column(nullable = false)
    private Integer correctAnswers;

    /**
     * Total de preguntas
     */
    @Column(nullable = false)
    private Integer totalQuestions;

    /**
     * Tiempo empleado en segundos
     */
    @Column(nullable = false)
    private Integer timeSpent;

    /**
     * Respuestas del estudiante
     */
    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    /**
     * Fecha de realización
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    /**
     * Entidad anidada para almacenar cada respuesta
     */
    @Entity
    @Table(name = "answers")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Answer {
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "result_id", nullable = false)
        private Result result;

        /**
         * ID de la pregunta
         */
        @Column(nullable = false)
        private Long questionId;

        /**
         * Índice de la respuesta del estudiante (0-3)
         */
        @Column(nullable = false)
        private Integer studentAnswer;

        /**
         * Índice de la respuesta correcta (0-3)
         */
        @Column(nullable = false)
        private Integer correctAnswer;

        /**
         * Si la respuesta fue correcta
         */
        @Column(nullable = false)
        private Boolean isCorrect;
    }
}
