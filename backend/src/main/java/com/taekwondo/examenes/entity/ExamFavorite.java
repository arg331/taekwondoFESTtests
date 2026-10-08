package com.taekwondo.examenes.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Un profesor marca como favorito un examen público de otro profesor.
 */
@Entity
@Table(name = "exam_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorites_professor_exam",
                columnNames = {"professor_id", "exam_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExamFavorite(Long professorId, Long examId) {
        this.professorId = professorId;
        this.examId = examId;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
