package com.taekwondo.examenes.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa una pregunta del examen de arbitraje
 */
@Entity
@Table(name = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Array de 4 opciones de respuesta
     * Lo almaceno como JSON en la base de datos
     */
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    @OrderColumn(name = "option_order")
    private List<String> options;

    /**
     * Índice de la respuesta correcta (0-3)
     */
    @Column(nullable = false)
    private Integer correctAnswer;

    /**
     * Explicación de la respuesta (opcional)
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

  
    public enum Category {
        PUNTUACION,
        REGLAMENTO,
        PENALIZACIONES,
        SITUACIONES_ESPECIALES
    }

    public enum Difficulty {
        FACIL,
        MEDIO,
        DIFICIL
    }
}