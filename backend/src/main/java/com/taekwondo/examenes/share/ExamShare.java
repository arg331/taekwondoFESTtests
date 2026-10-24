package com.taekwondo.examenes.share;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Un examen compartido con un profesor (RF-51). Mientras está pendiente es una invitación; al aceptarla,
 * el profesor consulta el examen y sus resultados igual que el dueño, sin poder cambiarlo.
 */
@Entity
@Table(name = "exam_shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShareStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public ExamShare(Long examId, Long teacherId, Long invitedBy, LocalDateTime createdAt) {
        this.examId = examId;
        this.teacherId = teacherId;
        this.invitedBy = invitedBy;
        this.status = ShareStatus.PENDING;
        this.createdAt = createdAt;
    }

    public boolean isPending() {
        return status == ShareStatus.PENDING;
    }

    public void accept(LocalDateTime at) {
        status = ShareStatus.ACCEPTED;
        respondedAt = at;
    }
}
