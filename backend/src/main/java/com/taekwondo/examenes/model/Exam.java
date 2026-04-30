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
 * Entidad que representa un examen generado
 */
@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único para acceder al examen (se usa en el QR)
     */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Número de preguntas del examen
     */
    @Column(nullable = false)
    private Integer numberOfQuestions;

    /**
     * Si se debe mostrar la nota al finalizar
     */
    @Column(nullable = false)
    private Boolean showScore;

    /**
     * Tiempo límite en minutos (0 = sin límite)
     */
    @Column(nullable = false)
    private Integer timeLimit;

    /**
     * Si las opciones deben aleatorizarse
     */
    @Column(nullable = false)
    private Boolean randomizeOptions;

    /**
     * IDs de las preguntas incluidas en este examen
     */
    @ElementCollection
    @CollectionTable(name = "exam_questions", joinColumns = @JoinColumn(name = "exam_id"))
    @Column(name = "question_id")
    @OrderColumn(name = "question_order")
    private List<Long> questionIds = new ArrayList<>();

    /**
     * Si el examen está activo
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Fecha de creación
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha de expiración (3 días después de crear)
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
