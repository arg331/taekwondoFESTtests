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
 * Entidad Exam - Exámenes generados
 * 
 * CAMBIOS vs versión anterior:
 * - Se agregó title (título del examen)
 * - Se agregó ownerId (profesor propietario)
 * - Se agregó isDraft (estado borrador vs publicado)
 * - Se agregó isPublic (privado vs público para otros profesores)
 * - Se agregó randomizeQuestionOrder (aleatorizar orden de preguntas)
 * - Se agregó allowedTags (tags permitidos para pre-generación)
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
    
    @Column(unique = true, nullable = false, length = 20)
    private String code; // Formato: EXM-XXXXXXXX (generado al publicar)
    
    @Column(nullable = false)
    private String title; // "Examen Final 2025", "Parcial Junio"
    
    @Column(nullable = false, name = "owner_id")
    private Long ownerId; // ID del profesor propietario
    
    @Column(nullable = false, name = "is_draft")
    private Boolean isDraft = true; 
    // true = en edición (vista previa)
    // false = publicado (estudiantes pueden acceder)
    
    @Column(nullable = false, name = "is_public")
    private Boolean isPublic = false;
    // true = visible para otros profesores (pueden favoritear/importar)
    // false = privado (solo el dueño lo ve)
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "exam_allowed_tags",
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> allowedTags = new HashSet<>();
    // Tags permitidos para pre-generar preguntas
    
    @Column(nullable = false, name = "number_of_questions")
    private Integer numberOfQuestions;
    
    @ElementCollection
    @CollectionTable(
        name = "exam_questions",
        joinColumns = @JoinColumn(name = "exam_id")
    )
    @Column(name = "question_id")
    @OrderColumn(name = "question_order")
    private List<Long> questionIds = new ArrayList<>();
    // NO ES FK - Modelo híbrido para inmutabilidad
    // El orden en esta lista importa (posición de la pregunta en el examen)
    
    @Column(nullable = false, name = "show_score")
    private Boolean showScore = true;
    // true = estudiante ve su nota al terminar
    // false = no se muestra la nota
    
    @Column(name = "time_limit")
    private Integer timeLimit; // En minutos, null = sin límite
    
    @Column(nullable = false, name = "randomize_options")
    private Boolean randomizeOptions = false;
    // true = opciones de cada pregunta se aleatorizan
    // false = opciones en orden original
    
    @Column(nullable = false, name = "randomize_question_order")
    private Boolean randomizeQuestionOrder = false;
    // true = preguntas se muestran en orden aleatorio
    // false = preguntas en el orden de questionIds
    
    @Column(nullable = false)
    private Boolean active = false;
    // Solo un examen puede estar activo a la vez
    // Se activa cuando isDraft = false
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    // Se establece al publicar: now() + 3 días
}
