package com.taekwondo.examenes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad ExamFavorite - Relación profesor ↔ examen favorito
 * 
 * Permite a los profesores marcar exámenes públicos como favoritos
 */
@Entity
@Table(
    name = "exam_favorites",
    uniqueConstraints = @UniqueConstraint(columnNames = {"professor_id", "exam_id"})
    // Un profesor no puede favoritear el mismo examen dos veces
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamFavorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, name = "professor_id")
    private Long professorId; // Quien marcó como favorito
    
    @Column(nullable = false, name = "exam_id")
    private Long examId; // Examen favoriteado (debe ser isPublic = true)
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructor sin ID
    public ExamFavorite(Long professorId, Long examId) {
        this.professorId = professorId;
        this.examId = examId;
    }
}
