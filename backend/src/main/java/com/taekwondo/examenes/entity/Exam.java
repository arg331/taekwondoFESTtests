package com.taekwondo.examenes.entity;

import com.taekwondo.examenes.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Examen creado por un profesor.
 *
 * Ciclo de vida: DRAFT → PUBLISHED ⇄ EXPIRED.
 * La configuración y las preguntas solo se pueden cambiar en DRAFT, para que
 * todos los alumnos de un examen publicado hagan exactamente el mismo examen.
 * La caducidad por fecha (expiresAt) no cambia el estado: se evalúa al vuelo.
 */
@Entity
@Table(name = "exams",
        uniqueConstraints = @UniqueConstraint(name = "uk_exams_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status = ExamStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 20)
    private ExamAccessMode accessMode = ExamAccessMode.OPEN;

    @Embedded
    private ExamConfig config;

    /** IDs de las preguntas, en orden. Sin FK: las preguntas viven en su propio agregado. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_questions", joinColumns = @JoinColumn(name = "exam_id"))
    @OrderColumn(name = "position")
    @Column(name = "question_id", nullable = false)
    private List<Long> questionIds = new ArrayList<>();

    /** Tags usados al pre-generar el examen (solo informativo). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "exam_generation_tags",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> generationTags = new HashSet<>();

    /** Código de acceso (QR). Solo existe una vez publicado. */
    @Column(length = 50)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Exam(String title, Long ownerId, ExamConfig config, Set<Tag> generationTags) {
        this.title = title;
        this.ownerId = ownerId;
        this.config = config;
        this.generationTags.addAll(generationTags);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ───── Edición ─────

    public void rename(String newTitle) {
        this.title = newTitle;
    }

    public void changeConfig(ExamConfig newConfig) {
        requireDraft("cambiar la configuración");
        this.config = newConfig;
    }

    public void replaceQuestions(List<Long> newQuestionIds) {
        requireDraft("cambiar las preguntas");
        questionIds.clear();
        questionIds.addAll(newQuestionIds);
    }

    public void removeQuestion(Long questionId) {
        requireDraft("quitar preguntas");
        questionIds.remove(questionId);
    }

    // ───── Transiciones de estado ─────

    public void publish(String code, Visibility visibility, ExamAccessMode accessMode,
                        LocalDateTime expiresAt) {
        requireDraft("publicar");
        if (questionIds.size() != config.getNumberOfQuestions()) {
            throw new BusinessRuleException("El examen debe tener " + config.getNumberOfQuestions()
                    + " preguntas para publicarse (tiene " + questionIds.size() + ")");
        }
        this.code = code;
        this.visibility = visibility;
        this.accessMode = accessMode;
        this.expiresAt = expiresAt;
        this.status = ExamStatus.PUBLISHED;
    }

    public void close() {
        if (status != ExamStatus.PUBLISHED) {
            throw new BusinessRuleException("Solo se pueden cerrar exámenes publicados (estado actual: " + status + ")");
        }
        status = ExamStatus.EXPIRED;
    }

    public void reopen(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.EXPIRED) {
            throw new BusinessRuleException("Solo se pueden reabrir exámenes cerrados (estado actual: " + status + ")");
        }
        expiresAt = newExpiresAt;
        status = ExamStatus.PUBLISHED;
    }

    public void changeExpiration(LocalDateTime newExpiresAt) {
        if (status != ExamStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Solo se puede cambiar la expiración de exámenes publicados (estado actual: " + status + ")");
        }
        expiresAt = newExpiresAt;
    }

    public void changeVisibility(Visibility newVisibility) {
        if (status == ExamStatus.DRAFT) {
            throw new BusinessRuleException("Un borrador no tiene visibilidad pública");
        }
        visibility = newVisibility;
    }

    // ───── Consultas ─────

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public boolean isDraft() {
        return status == ExamStatus.DRAFT;
    }

    public boolean requiresRegistration() {
        return accessMode == ExamAccessMode.REGISTERED_ONLY;
    }

    /** Publicado y, si tiene fecha de expiración, todavía no ha pasado. */
    public boolean isAccessibleAt(LocalDateTime now) {
        return status == ExamStatus.PUBLISHED && (expiresAt == null || !now.isAfter(expiresAt));
    }

    public List<Long> getQuestionIds() {
        return Collections.unmodifiableList(questionIds);
    }

    public Set<Tag> getGenerationTags() {
        return Collections.unmodifiableSet(generationTags);
    }

    private void requireDraft(String action) {
        if (status != ExamStatus.DRAFT) {
            throw new BusinessRuleException("No se puede " + action + ": el examen ya no está en borrador "
                    + "(estado actual: " + status + ")");
        }
    }
}
