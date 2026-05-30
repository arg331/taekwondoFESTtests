package com.taekwondo.examenes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidad ExamTemplate - Plantillas de exámenes para reutilización
 * 
 * Permite guardar configuraciones de examen para crear nuevos drafts rápidamente
 */
@Entity
@Table(name = "exam_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name; // "Examen Tipo A - Adultos", "Parcial Teoría Básica"
    
    @Column(columnDefinition = "TEXT")
    private String description; // Descripción opcional
    
    @Column(nullable = false, name = "owner_id")
    private Long ownerId; // Profesor propietario
    
    // Configuración guardada
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "template_allowed_tags",
        joinColumns = @JoinColumn(name = "template_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> allowedTags = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(
        name = "template_questions",
        joinColumns = @JoinColumn(name = "template_id")
    )
    @Column(name = "question_id")
    @OrderColumn(name = "question_order")
    private List<Long> questionIds = new ArrayList<>();
    
    @Column(nullable = false, name = "number_of_questions")
    private Integer numberOfQuestions;
    
    @Column(name = "time_limit")
    private Integer timeLimit;
    
    @Column(nullable = false, name = "show_score")
    private Boolean showScore = true;
    
    @Column(nullable = false, name = "randomize_options")
    private Boolean randomizeOptions = false;
    
    @Column(nullable = false, name = "randomize_question_order")
    private Boolean randomizeQuestionOrder = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}
