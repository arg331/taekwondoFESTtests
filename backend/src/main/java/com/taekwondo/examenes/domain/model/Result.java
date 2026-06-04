package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Entidad de dominio: Result (resultado de un estudiante en un examen).
 *
 * Es un AGREGADO: contiene un value object Answer por cada pregunta.
 *
 * Inmutabilidad: una vez creado, NO se edita.
 *
 * Identidad del estudiante:
 *  - Si studentUserId != null  → estudiante registrado (cuenta verificada)
 *  - Si studentUserId == null  → estudiante anónimo (solo aportó nombre)
 *
 * En ambos casos se guardan studentName, studentClub y studentEmail
 * porque el estudiante los introdujo manualmente al hacer el examen.
 */
public final class Result {

    private static final int PASSING_SCORE = 70;

    private final Long id;
    private final Long examId;
    private final Long studentUserId;        // null si anónimo
    private final String studentName;
    private final String studentClub;
    private final String studentEmail;
    private final List<Answer> answers;
    private final int correctAnswers;
    private final int totalQuestions;
    private final int score;
    private final int timeSpentSeconds;
    private final LocalDateTime completedAt;

    // ──────────────────────────────────────────────────
    // Factory methods
    // ──────────────────────────────────────────────────

    public static Result createNew(Long examId,
                                    Long studentUserId,
                                    String studentName,
                                    String studentClub,
                                    String studentEmail,
                                    List<Answer> answers,
                                    int timeSpentSeconds,
                                    LocalDateTime completedAt) {
        validateExamId(examId);
        validateStudentName(studentName);
        validateAnswers(answers);
        validateTimeSpent(timeSpentSeconds);
        Objects.requireNonNull(completedAt, "completedAt no puede ser null");

        int total = answers.size();
        int correct = (int) answers.stream().filter(Answer::isCorrect).count();
        int computedScore = total == 0 ? 0 : Math.round((correct * 100f) / total);

        return new Result(
                null, examId, studentUserId,
                studentName, studentClub, studentEmail,
                new ArrayList<>(answers),
                correct, total, computedScore,
                timeSpentSeconds, completedAt
        );
    }

    public static Result reconstitute(Long id,
                                       Long examId,
                                       Long studentUserId,
                                       String studentName,
                                       String studentClub,
                                       String studentEmail,
                                       List<Answer> answers,
                                       int correctAnswers,
                                       int totalQuestions,
                                       int score,
                                       int timeSpentSeconds,
                                       LocalDateTime completedAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new Result(
                id, examId, studentUserId,
                studentName, studentClub, studentEmail,
                new ArrayList<>(answers),
                correctAnswers, totalQuestions, score,
                timeSpentSeconds, completedAt
        );
    }

    private Result(Long id, Long examId, Long studentUserId,
                   String studentName, String studentClub, String studentEmail,
                   List<Answer> answers, int correctAnswers, int totalQuestions,
                   int score, int timeSpentSeconds, LocalDateTime completedAt) {
        this.id = id;
        this.examId = examId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.studentClub = studentClub;
        this.studentEmail = studentEmail;
        this.answers = answers;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.score = score;
        this.timeSpentSeconds = timeSpentSeconds;
        this.completedAt = completedAt;
    }

    // ──────────────────────────────────────────────────
    // Consultas de negocio
    // ──────────────────────────────────────────────────

    public boolean isPassed()       { return score >= PASSING_SCORE; }
    public boolean isVerified()     { return studentUserId != null; }

    // ──────────────────────────────────────────────────
    // Validaciones
    // ──────────────────────────────────────────────────

    private static void validateExamId(Long examId) {
        Objects.requireNonNull(examId, "examId no puede ser null");
    }

    private static void validateStudentName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del estudiante no puede estar vacío");
        }
    }

    private static void validateAnswers(List<Answer> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Un resultado debe contener al menos una respuesta");
        }
    }

    private static void validateTimeSpent(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("El tiempo empleado no puede ser negativo");
        }
    }

    // ──────────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────────

    public Long getId()                     { return id; }
    public Long getExamId()                 { return examId; }
    public Long getStudentUserId()          { return studentUserId; }
    public String getStudentName()          { return studentName; }
    public String getStudentClub()          { return studentClub; }
    public String getStudentEmail()         { return studentEmail; }
    public List<Answer> getAnswers()        { return Collections.unmodifiableList(answers); }
    public int getCorrectAnswers()          { return correctAnswers; }
    public int getTotalQuestions()          { return totalQuestions; }
    public int getScore()                   { return score; }
    public int getTimeSpentSeconds()        { return timeSpentSeconds; }
    public LocalDateTime getCompletedAt()   { return completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Result r)) return false;
        return Objects.equals(id, r.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
