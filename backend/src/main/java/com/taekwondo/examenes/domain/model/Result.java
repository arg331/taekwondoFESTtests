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
 * No hay forma de modificar las answers desde fuera: son parte del
 * Result y se establecen al crearlo.
 *
 * Inmutabilidad: una vez creado un Result, NO se edita. Representa
 * un evento ocurrido (el estudiante terminó el examen). No tiene
 * sentido modificarlo posteriormente.
 *
 * Snapshot histórico: el Result guarda examId, no Exam. Si el examen
 * cambia más tarde, el resultado conserva su contenido original
 * (preguntas, respuestas correctas en el momento, etc.).
 */
public final class Result {

    private final Long id;
    private final Long examId;
    private final String studentName;
    private final String studentClub;        // opcional
    private final String studentEmail;       // opcional
    private final List<Answer> answers;
    private final int correctAnswers;        // derivado pero persistido por eficiencia
    private final int totalQuestions;
    private final int score;                 // 0-100, derivado pero persistido
    private final int timeSpentSeconds;
    private final LocalDateTime completedAt;

    // ──────────────────────────────────────────────────
    // Factory methods
    // ──────────────────────────────────────────────────

    /**
     * Crea un Result nuevo (sin ID asignado todavía).
     * El score y correctAnswers se calculan a partir de las answers.
     */
    public static Result createNew(Long examId,
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
                null,
                examId,
                studentName,
                studentClub,
                studentEmail,
                new ArrayList<>(answers),
                correct,
                total,
                computedScore,
                timeSpentSeconds,
                completedAt
        );
    }

    /**
     * Reconstruye un Result desde la BD (con ID y campos derivados ya calculados).
     */
    public static Result reconstitute(Long id,
                                       Long examId,
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
                id, examId, studentName, studentClub, studentEmail,
                new ArrayList<>(answers),
                correctAnswers, totalQuestions, score,
                timeSpentSeconds, completedAt
        );
    }

    private Result(Long id, Long examId, String studentName, String studentClub,
                   String studentEmail, List<Answer> answers, int correctAnswers,
                   int totalQuestions, int score, int timeSpentSeconds,
                   LocalDateTime completedAt) {
        this.id = id;
        this.examId = examId;
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

    /**
     * Considera "aprobado" a partir del 70%.
     * Constante del dominio: si la federación cambia el umbral,
     * solo hay que tocar este punto.
     */
    private static final int PASSING_SCORE = 70;

    public boolean isPassed() {
        return score >= PASSING_SCORE;
    }

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
