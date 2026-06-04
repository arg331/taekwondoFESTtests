package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para ExamFavorite.
 *
 * Decisiones de modelado:
 *  - Sin FK con users ni exams: enlace débil. Si un examen se borra,
 *    los favoritos huérfanos se filtran al listar (decisión del caso de uso).
 *  - UNIQUE (professor_id, exam_id): defensa en profundidad contra
 *    race conditions, aunque el caso de uso ya valide duplicados.
 *  - Sin colecciones: entidad plana, no es agregado.
 */
@Entity
@Table(name = "exam_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorites_professor_exam",
                columnNames = {"professor_id", "exam_id"}))
public class ExamFavoriteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ExamFavoriteJpaEntity() {
        // Requerido por JPA
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProfessorId() { return professorId; }
    public void setProfessorId(Long professorId) { this.professorId = professorId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
