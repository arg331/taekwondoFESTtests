package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad de dominio: Exam.
 *
 * Un examen vive en dos estados principales:
 *  - DRAFT: el profesor lo construye y lo ajusta libremente
 *  - PUBLISHED: ya tiene código de acceso, lo pueden hacer los estudiantes,
 *               y es INMUTABLE (snapshot histórico)
 *
 * El examen guarda IDs de preguntas (no objetos completos) para garantizar
 * la inmutabilidad: si el profesor edita una pregunta más tarde, los exámenes
 * publicados que la contenían conservan su histórico (la pregunta cambia,
 * pero el examen ya guardó su contenido en el momento del examen del alumno).
 */
public final class Exam {

    private final Long id;
    private String title;
    private final Long ownerId;
    private ExamStatus status;
    private Visibility visibility;
    private ExamConfig config;
    private final List<Long> questionIds;
    private final Set<Tag> generationTags;   // tags usados al pre-generar (referencia)
    private String code;                      // null hasta publicar
    private final LocalDateTime createdAt;
    private LocalDateTime expiresAt;          // null hasta publicar

    // ──────────────────────────────────────────────────
    // Factory methods
    // ──────────────────────────────────────────────────

    public static Exam createDraft(String title,
                                    Long ownerId,
                                    ExamConfig config,
                                    Set<Tag> generationTags) {
        validateTitle(title);
        validateOwner(ownerId);
        Objects.requireNonNull(config, "config es obligatorio");

        return new Exam(
                null,
                title,
                ownerId,
                ExamStatus.DRAFT,
                Visibility.PRIVATE,
                config,
                new ArrayList<>(),
                new HashSet<>(generationTags != null ? generationTags : Set.of()),
                null,
                LocalDateTime.now(),
                null
        );
    }

    public static Exam reconstitute(Long id,
                                     String title,
                                     Long ownerId,
                                     ExamStatus status,
                                     Visibility visibility,
                                     ExamConfig config,
                                     List<Long> questionIds,
                                     Set<Tag> generationTags,
                                     String code,
                                     LocalDateTime createdAt,
                                     LocalDateTime expiresAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new Exam(
                id, title, ownerId, status, visibility, config,
                new ArrayList<>(questionIds),
                new HashSet<>(generationTags),
                code, createdAt, expiresAt
        );
    }

    private Exam(Long id,
                 String title,
                 Long ownerId,
                 ExamStatus status,
                 Visibility visibility,
                 ExamConfig config,
                 List<Long> questionIds,
                 Set<Tag> generationTags,
                 String code,
                 LocalDateTime createdAt,
                 LocalDateTime expiresAt) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.status = status;
        this.visibility = visibility;
        this.config = config;
        this.questionIds = questionIds;
        this.generationTags = generationTags;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // ──────────────────────────────────────────────────
    // Operaciones de negocio
    // ──────────────────────────────────────────────────

    /** Solo se puede modificar mientras está en DRAFT. */
    public void rename(String newTitle) {
        ensureDraft();
        validateTitle(newTitle);
        this.title = newTitle;
    }

    public void changeConfig(ExamConfig newConfig) {
        ensureDraft();
        Objects.requireNonNull(newConfig, "config no puede ser null");
        this.config = newConfig;
    }

    /**
     * Reemplaza completamente la lista de preguntas del draft.
     * El orden importa: define el orden de las preguntas en el examen.
     */
    public void setQuestions(List<Long> newQuestionIds) {
        ensureDraft();
        Objects.requireNonNull(newQuestionIds, "newQuestionIds no puede ser null");
        if (newQuestionIds.contains(null)) {
            throw new IllegalArgumentException("La lista de preguntas no puede contener null");
        }
        this.questionIds.clear();
        this.questionIds.addAll(newQuestionIds);
    }

    public void addQuestion(Long questionId) {
        ensureDraft();
        Objects.requireNonNull(questionId, "questionId no puede ser null");
        if (questionIds.contains(questionId)) {
            throw new IllegalArgumentException("La pregunta ya está en el examen");
        }
        this.questionIds.add(questionId);
    }

    public void removeQuestion(Long questionId) {
        ensureDraft();
        this.questionIds.remove(questionId);
    }

    /**
     * Publica el examen.
     *
     * @param code        código único de acceso (generado fuera del dominio)
     * @param visibility  visibilidad inicial (puede cambiarse después)
     * @param expiresAt   fecha de expiración
     */
    public void publish(String code, Visibility visibility, LocalDateTime expiresAt) {
        ensureDraft();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de publicación es obligatorio");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("La visibilidad es obligatoria");
        }
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de expiración debe ser futura");
        }
        if (questionIds.size() != config.getNumberOfQuestions()) {
            throw new IllegalStateException(
                    "El examen debe tener " + config.getNumberOfQuestions()
                            + " preguntas para publicarse (tiene " + questionIds.size() + ")");
        }

        this.code = code;
        this.visibility = visibility;
        this.expiresAt = expiresAt;
        this.status = ExamStatus.PUBLISHED;
    }

    /** Marca el examen como expirado. Solo aplicable si estaba PUBLISHED. */
    public void expire() {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException("Solo se pueden expirar exámenes publicados");
        }
        this.status = ExamStatus.EXPIRED;
    }

    /** Cambia la visibilidad de un examen publicado. */
    public void changeVisibility(Visibility newVisibility) {
        if (status == ExamStatus.DRAFT) {
            throw new IllegalStateException("Un draft no tiene visibilidad pública");
        }
        Objects.requireNonNull(newVisibility, "visibility no puede ser null");
        this.visibility = newVisibility;
    }

    public boolean isDraft()       { return status == ExamStatus.DRAFT; }
    public boolean isPublished()   { return status == ExamStatus.PUBLISHED; }
    public boolean isExpired()     { return status == ExamStatus.EXPIRED; }
    public boolean isAccessible()  { return status == ExamStatus.PUBLISHED; }

    // ──────────────────────────────────────────────────
    // Validaciones y guardas
    // ──────────────────────────────────────────────────

    private void ensureDraft() {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException(
                    "Esta operación solo es válida en estado DRAFT (estado actual: " + status + ")");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título del examen no puede estar vacío");
        }
    }

    private static void validateOwner(Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("El examen debe tener un propietario");
        }
    }

    // ──────────────────────────────────────────────────
    // Getters (sin setters: cambios via métodos de negocio)
    // ──────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getTitle()             { return title; }
    public Long getOwnerId()             { return ownerId; }
    public ExamStatus getStatus()        { return status; }
    public Visibility getVisibility()    { return visibility; }
    public ExamConfig getConfig()        { return config; }
    public List<Long> getQuestionIds()   { return Collections.unmodifiableList(questionIds); }
    public Set<Tag> getGenerationTags()  { return Collections.unmodifiableSet(generationTags); }
    public String getCode()              { return code; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getExpiresAt()  { return expiresAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exam e)) return false;
        return Objects.equals(id, e.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}