package com.taekwondo.examenes.result;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entrega de un examen por un alumno. Inmutable una vez creada.
 * studentUserId es null cuando el alumno es anónimo.
 */
@Entity
@Table(name = "results", indexes = @Index(name = "idx_results_exam", columnList = "exam_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Result {

    public static final int PASSING_SCORE = 70;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "result_answers", joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "position")
    private List<Answer> answers = new ArrayList<>();

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    /** Porcentaje de aciertos, 0..100. */
    @Column(nullable = false)
    private int score;

    @Column(name = "time_spent_seconds", nullable = false)
    private int timeSpentSeconds;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public Result(Long examId, Long studentUserId, String studentName, String studentClub,
                  String studentEmail, List<Answer> answers, int timeSpentSeconds,
                  LocalDateTime completedAt) {
        this.examId = examId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.studentClub = studentClub;
        this.studentEmail = studentEmail;
        this.answers.addAll(answers);
        this.totalQuestions = answers.size();
        this.correctAnswers = (int) answers.stream().filter(Answer::isCorrect).count();
        this.score = totalQuestions == 0 ? 0 : Math.round(correctAnswers * 100f / totalQuestions);
        this.timeSpentSeconds = timeSpentSeconds;
        this.completedAt = completedAt;
    }

    public boolean isPassed() {
        return score >= PASSING_SCORE;
    }

    public List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}
