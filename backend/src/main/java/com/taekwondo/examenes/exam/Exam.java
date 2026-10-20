package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.question.Question;
import com.taekwondo.examenes.tag.Tag;
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
 * Al publicar se copian las preguntas (DE-03): editar el banco después no cambia el examen.
 * La caducidad por fecha (expiresAt) no cambia el estado: se evalúa al vuelo.
 */
@Entity
@Table(name = "exams",
        uniqueConstraints = @UniqueConstraint(name = "uk_exams_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exam {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MIN_ATTEMPTS = 1;
    public static final int MAX_ATTEMPTS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
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

    /** Intentos por alumno (RF-52): de 1 a 4, o null = sin límite. Se fija al publicar y se puede cambiar. */
    @Column(name = "max_attempts")
    private Integer maxAttempts = MIN_ATTEMPTS;

    /** IDs de las preguntas, en orden. Sin FK: las preguntas viven en su propio agregado. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exam_questions", joinColumns = @JoinColumn(name = "exam_id"))
    @OrderColumn(name = "position")
    @Column(name = "question_id", nullable = false)
    private List<Long> questionIds = new ArrayList<>();

    /** Copia de las preguntas al publicar, en orden (DE-03). Vacía en borrador. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "exam_id", nullable = false, updatable = false)
    @OrderBy("position")
    private List<PublishedQuestion> publishedQuestions = new ArrayList<>();

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

    /**
     * Borrador nuevo con la misma configuración, preguntas y tags de generación (RF-18).
     * No hereda estado, código, visibilidad ni plazo: se publica como cualquier borrador.
     */
    public Exam copyAs(String newTitle) {
        ExamConfig configCopy = new ExamConfig(config.getNumberOfQuestions(), config.getTimeLimitMinutes(),
                config.isShowScore(), config.isShowAnswers(), config.isRandomizeOptions(),
                config.isRandomizeQuestionOrder());
        Exam copy = new Exam(newTitle, ownerId, configCopy, generationTags);
        copy.questionIds.addAll(questionIds);
        return copy;
    }

    // ───── Transiciones de estado ─────

    /** Publica con las preguntas del banco en el orden del examen; se guarda una copia de cada una. */
    public void publish(String code, Visibility visibility, ExamAccessMode accessMode, LocalDateTime expiresAt,
                        Integer maxAttempts, List<Question> questionsInOrder) {
        requireDraft("publicar");
        requireAttemptsInRange(maxAttempts);
        int count = questionIds.size();
        if (count < ExamConfig.MIN_QUESTIONS || count > ExamConfig.MAX_QUESTIONS) {
            throw new BusinessRuleException("El examen debe tener entre " + ExamConfig.MIN_QUESTIONS + " y "
                    + ExamConfig.MAX_QUESTIONS + " preguntas para publicarse (tiene " + count + ")");
        }
        // El número de preguntas es el de las elegidas (RF-46)
        config.matchQuestionCount(count);
        if (!questionsInOrder.stream().map(Question::getId).toList().equals(questionIds)) {
            throw new IllegalArgumentException("Las preguntas a copiar no son las del examen");
        }
        for (int i = 0; i < count; i++) {
            publishedQuestions.add(PublishedQuestion.copyOf(i, questionsInOrder.get(i)));
        }
        this.code = code;
        this.visibility = visibility;
        this.accessMode = accessMode;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
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

    /** Subir o bajar los intentos de un examen ya publicado; null = sin límite. */
    public void changeMaxAttempts(Integer newMaxAttempts) {
        if (status == ExamStatus.DRAFT) {
            throw new BusinessRuleException("Los intentos de un borrador se eligen al publicarlo");
        }
        requireAttemptsInRange(newMaxAttempts);
        maxAttempts = newMaxAttempts;
    }

    /** ¿Ha gastado ya todos sus intentos quien lleva usedAttempts? Sin límite, nunca. */
    public boolean attemptsUsedUp(int usedAttempts) {
        return maxAttempts != null && usedAttempts >= maxAttempts;
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

    public List<PublishedQuestion> getPublishedQuestions() {
        return Collections.unmodifiableList(publishedQuestions);
    }

    public Set<Tag> getGenerationTags() {
        return Collections.unmodifiableSet(generationTags);
    }

    private static void requireAttemptsInRange(Integer attempts) {
        if (attempts != null && (attempts < MIN_ATTEMPTS || attempts > MAX_ATTEMPTS)) {
            throw new IllegalArgumentException("Intentos fuera de rango: " + attempts);
        }
    }

    private void requireDraft(String action) {
        if (status != ExamStatus.DRAFT) {
            throw new BusinessRuleException("No se puede " + action + ": el examen ya no está en borrador "
                    + "(estado actual: " + status + ")");
        }
    }
}
