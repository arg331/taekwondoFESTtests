package com.taekwondo.examenes.attempt;

import com.taekwondo.examenes.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Intento de un alumno en un examen (DE-04). Se registra al empezar, con quién lo hace,
 * y se marca al entregarlo. Un intento sin entregar y fuera de plazo cuenta como gastado.
 * studentUserId es null cuando el alumno es anónimo: entonces se le reconoce por el nombre.
 */
@Entity
@Table(name = "exam_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_user_id")
    private Long studentUserId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "student_club", length = 100)
    private String studentClub;

    @Column(name = "student_email", length = 200)
    private String studentEmail;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    public ExamAttempt(Long examId, Long studentUserId, String studentName, String studentClub,
                       String studentEmail, LocalDateTime startedAt) {
        this.examId = examId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.studentClub = studentClub;
        this.studentEmail = studentEmail;
        this.startedAt = startedAt;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /** Sin entregar y dentro de su plazo: todavía se puede contestar y entregar. */
    public boolean isOpenAt(LocalDateTime now, Duration validity) {
        return !isSubmitted() && !now.isAfter(startedAt.plus(validity));
    }

    public void markSubmitted(LocalDateTime at) {
        if (isSubmitted()) {
            throw new BusinessRuleException("Este intento ya se ha entregado");
        }
        submittedAt = at;
    }
}
