package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para Result (snapshot histórico inmutable).
 *
 * Decisiones de modelado:
 *  - examId guardado como Long (sin FK a ExamJpaEntity): mantenemos
 *    enlace débil para que el Result sobreviva si el examen se borra
 *    en el futuro (acordado por diseño).
 *  - studentUserId NULLABLE, sin FK: anónimos no tienen userId; y si
 *    un user se borra, el resultado histórico sobrevive.
 *  - correctAnswers, totalQuestions, score: persistidos como columnas
 *    para que el contrato reconstitute(...) del dominio funcione tal cual
 *    y queries SQL de estadísticas sean eficientes.
 *  - answers: @ElementCollection de @Embeddable con @OrderColumn,
 *    misma estrategia que QuestionJpaEntity.options. FetchType.EAGER
 *    porque ResultView.from() siempre los necesita.
 *  - Sin índice especial: las consultas más frecuentes son por examId
 *    y studentUserId. Se añadirán índices cuando el volumen lo justifique.
 */
@Entity
@Table(name = "results")
public class ResultJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_user_id")  // nullable: anónimos
    private Long studentUserId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "student_club", length = 100)
    private String studentClub;

    @Column(name = "student_email", length = 200)
    private String studentEmail;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "result_answers",
            joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "position")
    private List<AnswerJpaEmbeddable> answers = new ArrayList<>();

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int score;

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public ResultJpaEntity() {
        // Requerido por JPA
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentClub() { return studentClub; }
    public void setStudentClub(String studentClub) { this.studentClub = studentClub; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public List<AnswerJpaEmbeddable> getAnswers() { return answers; }
    public void setAnswers(List<AnswerJpaEmbeddable> answers) { this.answers = answers; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(int timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
