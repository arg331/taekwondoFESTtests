package com.taekwondo.examenes.domain.model;

import com.taekwondo.examenes.domain.port.Clock;

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
 * Estados:
 *  - DRAFT     → el profesor lo construye y lo ajusta
 *  - PUBLISHED → tiene código, los estudiantes pueden hacerlo
 *  - EXPIRED   → cerrado MANUALMENTE por el profesor
 *
 * Modo de acceso:
 *  - OPEN              → anónimos + registrados
 *  - REGISTERED_ONLY   → solo estudiantes con cuenta
 *
 * Sobre la expiración por TIEMPO: NO se persiste como estado EXPIRED.
 * El campo expiresAt indica hasta cuándo es accesible. La pregunta
 * "¿está accesible ahora mismo?" se calcula on-the-fly comparando
 * con un Clock.
 */
public final class Exam {

    private final Long id;
    private String title;
    private final Long ownerId;
    private ExamStatus status;
    private Visibility visibility;
    private ExamAccessMode accessMode;
    private ExamConfig config;
    private final List<Long> questionIds;
    private final Set<Tag> generationTags;
    private String code;
    private final LocalDateTime createdAt;
    private LocalDateTime expiresAt;

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
                null, title, ownerId,
                ExamStatus.DRAFT,
                Visibility.PRIVATE,
                ExamAccessMode.OPEN,
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
                                     ExamAccessMode accessMode,
                                     ExamConfig config,
                                     List<Long> questionIds,
                                     Set<Tag> generationTags,
                                     String code,
                                     LocalDateTime createdAt,
                                     LocalDateTime expiresAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new Exam(
                id, title, ownerId, status, visibility, accessMode, config,
                new ArrayList<>(questionIds),
                new HashSet<>(generationTags),
                code, createdAt, expiresAt
        );
    }

    private Exam(Long id, String title, Long ownerId,
                 ExamStatus status, Visibility visibility, ExamAccessMode accessMode,
                 ExamConfig config, List<Long> questionIds, Set<Tag> generationTags,
                 String code, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.title = title;
        this.ownerId = ownerId;
        this.status = status;
        this.visibility = visibility;
        this.accessMode = accessMode;
        this.config = config;
        this.questionIds = questionIds;
        this.generationTags = generationTags;
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // ──────────────────────────────────────────────────
    // Operaciones de edición (válidas en cualquier estado)
    // ──────────────────────────────────────────────────

    public void rename(String newTitle) {
        validateTitle(newTitle);
        this.title = newTitle;
    }

    public void changeConfig(ExamConfig newConfig) {
        Objects.requireNonNull(newConfig, "config no puede ser null");
        this.config = newConfig;
    }

    public void setQuestions(List<Long> newQuestionIds) {
        Objects.requireNonNull(newQuestionIds, "newQuestionIds no puede ser null");
        if (newQuestionIds.contains(null)) {
            throw new IllegalArgumentException("La lista de preguntas no puede contener null");
        }
        this.questionIds.clear();
        this.questionIds.addAll(newQuestionIds);
    }

    public void addQuestion(Long questionId) {
        Objects.requireNonNull(questionId, "questionId no puede ser null");
        if (questionIds.contains(questionId)) {
            throw new IllegalArgumentException("La pregunta ya está en el examen");
        }
        this.questionIds.add(questionId);
    }

    public void removeQuestion(Long questionId) {
        this.questionIds.remove(questionId);
    }

    public void changeAccessMode(ExamAccessMode newAccessMode) {
        Objects.requireNonNull(newAccessMode, "accessMode no puede ser null");
        this.accessMode = newAccessMode;
    }

    // ──────────────────────────────────────────────────
    // Transiciones de estado
    // ──────────────────────────────────────────────────

    public void publish(String code, Visibility visibility, LocalDateTime expiresAt) {
        if (status != ExamStatus.DRAFT) {
            throw new IllegalStateException(
                    "Solo se puede publicar un examen en estado DRAFT (estado actual: " + status + ")");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de publicación es obligatorio");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("La visibilidad es obligatoria");
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

    public void closeManually() {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Solo se pueden cerrar exámenes publicados (estado actual: " + status + ")");
        }
        this.status = ExamStatus.EXPIRED;
    }

    public void reopen(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Solo se pueden reabrir exámenes expirados (estado actual: " + status + ")");
        }
        this.expiresAt = newExpiresAt;
        this.status = ExamStatus.PUBLISHED;
    }

    public void extendExpiration(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Solo se puede extender la expiración de exámenes publicados (estado actual: " + status + ")");
        }
        this.expiresAt = newExpiresAt;
    }

    public void changeVisibility(Visibility newVisibility) {
        if (status == ExamStatus.DRAFT) {
            throw new IllegalStateException("Un draft no tiene visibilidad pública");
        }
        Objects.requireNonNull(newVisibility, "visibility no puede ser null");
        this.visibility = newVisibility;
    }

    // ──────────────────────────────────────────────────
    // Consultas
    // ──────────────────────────────────────────────────

    public boolean isDraft()              { return status == ExamStatus.DRAFT; }
    public boolean isPublished()          { return status == ExamStatus.PUBLISHED; }
    public boolean isExpired()            { return status == ExamStatus.EXPIRED; }
    public boolean requiresRegistration() { return accessMode == ExamAccessMode.REGISTERED_ONLY; }

    public boolean isCurrentlyExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock no puede ser null");
        if (status == ExamStatus.EXPIRED) return true;
        if (status != ExamStatus.PUBLISHED) return false;
        if (expiresAt == null) return false;
        return clock.now().isAfter(expiresAt);
    }

    public boolean isAccessibleAt(Clock clock) {
        if (status != ExamStatus.PUBLISHED) return false;
        if (expiresAt == null) return true;
        return !clock.now().isAfter(expiresAt);
    }

    // ──────────────────────────────────────────────────
    // Validaciones
    // ──────────────────────────────────────────────────

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
    // Getters
    // ──────────────────────────────────────────────────

    public Long getId()                     { return id; }
    public String getTitle()                { return title; }
    public Long getOwnerId()                { return ownerId; }
    public ExamStatus getStatus()           { return status; }
    public Visibility getVisibility()       { return visibility; }
    public ExamAccessMode getAccessMode()   { return accessMode; }
    public ExamConfig getConfig()           { return config; }
    public List<Long> getQuestionIds()      { return Collections.unmodifiableList(questionIds); }
    public Set<Tag> getGenerationTags()     { return Collections.unmodifiableSet(generationTags); }
    public String getCode()                 { return code; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public LocalDateTime getExpiresAt()     { return expiresAt; }

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
