package com.taekwondo.examenes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidad Question - Preguntas del banco
 * 
 * CAMBIOS vs versión anterior:
 * - Se eliminó el enum Category
 * - Se agregó relación ManyToMany con Tag
 * - Se agregó ownerId (profesor propietario)
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
    
    @Column(nullable = false, name = "owner_id")
    private Long ownerId; // ID del profesor propietario
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "question_tags",
        joinColumns = @JoinColumn(name = "question_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
    
    @ElementCollection
    @CollectionTable(
        name = "question_options",
        joinColumns = @JoinColumn(name = "question_id")
    )
    @Column(name = "option_text", columnDefinition = "TEXT")
    @OrderColumn(name = "option_order")
    private List<String> options = new ArrayList<>(); // Debe tener exactamente 4
    
    @Column(nullable = false)
    private Integer correctAnswer; // 0-3 (índice de la opción correcta)
    
    @Column(columnDefinition = "TEXT")
    private String explanation; // Explicación opcional
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Enum Difficulty
    public enum Difficulty {
        FACIL, MEDIO, DIFICIL
    }
}
