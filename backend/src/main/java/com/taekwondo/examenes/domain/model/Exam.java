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
 * Sobre la expiración por TIEMPO: NO se persiste como estado EXPIRED.
 * El campo expiresAt indica hasta cuándo es accesible. La pregunta
 * "¿está accesible ahora mismo?" se calcula on-the-fly comparando
 * con un Clock (ver isAccessibleAt / isCurrentlyExpired).
 *
 * Sobre la edición: el profesor puede editar el examen incluso después
 * de publicarlo. Esto es una decisión consciente: los exámenes son
 * típicamente privados, los resultados ya almacenados quedan como log
 * histórico aunque la pregunta cambie. Sí se podrá publicar/cerrar/
 * reabrir según el estado.
 *
 * El examen guarda IDs de preguntas, no objetos completos (modelo híbrido).
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
    private String code;                      // null hasta publicar; tras publicar, persiste
    private final LocalDateTime createdAt;
    private LocalDateTime expiresAt;          // null = no expira por tiempo

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

    // ──────────────────────────────────────────────────
    // Transiciones de estado
    // ──────────────────────────────────────────────────

    /**
     * Publica el examen desde DRAFT.
     *
     * @param code        código único de acceso (generado por ExamCodeGenerator)
     * @param visibility  visibilidad inicial
     * @param expiresAt   fecha de expiración (puede ser null = no expira por tiempo)
     */
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
        // expiresAt puede ser null (sin expiración por tiempo).
        // Si no lo es, debe ser futuro: no tiene sentido publicar algo ya expirado.
        // Esta validación se hará en el caso de uso usando el Clock.

        this.code = code;
        this.visibility = visibility;
        this.expiresAt = expiresAt;
        this.status = ExamStatus.PUBLISHED;
    }

    /**
     * El profesor cierra el examen manualmente.
     * Solo aplicable si está PUBLISHED.
     */
    public void closeManually() {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Solo se pueden cerrar exámenes publicados (estado actual: " + status + ")");
        }
        this.status = ExamStatus.EXPIRED;
    }

    /**
     * Reabre un examen cerrado manualmente.
     * Extiende la fecha de expiración con el valor dado.
     *
     * @param newExpiresAt nueva fecha de expiración (puede ser null = sin límite)
     */
    public void reopen(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.EXPIRED) {
            throw new IllegalStateException(
                    "Solo se pueden reabrir exámenes expirados (estado actual: " + status + ")");
        }
        this.expiresAt = newExpiresAt;
        this.status = ExamStatus.PUBLISHED;
    }

    /**
     * Extiende la fecha de expiración mientras el examen está PUBLISHED.
     * Útil cuando el examen sigue activo pero el profesor quiere darle más tiempo.
     */
    public void extendExpiration(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.PUBLISHED) {
            throw new IllegalStateException(
                    "Solo se puede extender la expiración de exámenes publicados (estado actual: " + status + ")");
        }
        this.expiresAt = newExpiresAt;
    }

    /**
     * Cambia la visibilidad de un examen ya publicado (o expirado).
     */
    public void changeVisibility(Visibility newVisibility) {
        if (status == ExamStatus.DRAFT) {
            throw new IllegalStateException("Un draft no tiene visibilidad pública");
        }
        Objects.requireNonNull(newVisibility, "visibility no puede ser null");
        this.visibility = newVisibility;
    }

    // ──────────────────────────────────────────────────
    // Consultas de estado (on-the-fly)
    // ──────────────────────────────────────────────────

    public boolean isDraft()     { return status == ExamStatus.DRAFT; }
    public boolean isPublished() { return status == ExamStatus.PUBLISHED; }
    public boolean isExpired()   { return status == ExamStatus.EXPIRED; }

    /**
     * ¿Está realmente cerrado por tiempo, aunque su estado siga siendo PUBLISHED?
     * Cálculo on-the-fly basado en el Clock.
     */
    public boolean isCurrentlyExpired(Clock clock) {
        Objects.requireNonNull(clock, "clock no puede ser null");
        if (status == ExamStatus.EXPIRED) return true;
        if (status != ExamStatus.PUBLISHED) return false;
        if (expiresAt == null) return false;   // sin fecha = no expira
        return clock.now().isAfter(expiresAt);
    }

    /**
     * ¿Puede un estudiante acceder al examen AHORA?
     * Combina el estado y la fecha de expiración.
     */
    public boolean isAccessibleAt(Clock clock) {
        if (status != ExamStatus.PUBLISHED) return false;
        if (expiresAt == null) return true;     // sin fecha de expiración = abierto siempre
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