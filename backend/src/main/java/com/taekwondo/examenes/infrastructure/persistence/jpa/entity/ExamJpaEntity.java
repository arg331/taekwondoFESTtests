package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import com.taekwondo.examenes.domain.model.ExamAccessMode;
import com.taekwondo.examenes.domain.model.ExamStatus;
import com.taekwondo.examenes.domain.model.Visibility;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidad JPA para Exam.
 *
 * Decisiones de modelado:
 *  - Estado, visibilidad y modo de acceso se guardan como STRING (legible
 *    en la BD; resistente a renombres de enum constants en futuro).
 *  - config como @Embedded: los 5 campos van como columnas planas.
 *  - questionIds como @ElementCollection con @OrderColumn: lista ordenada
 *    de Longs en tabla aparte (exam_questions). Sin relación JPA con
 *    QuestionJpaEntity (acoplamiento débil, decisión de dominio).
 *  - generationTags como @ManyToMany con TagJpaEntity: relación bidireccional
 *    NO, solo desde Exam. Tabla aparte (exam_generation_tags). FetchType.EAGER
 *    para que ExamView.from() funcione sin más.
 *  - code: nullable (drafts no tienen) y unique (cuando lo tienen).
 */
@Entity
@Table(name = "exams",
        uniqueConstraints = @UniqueConstraint(name = "uk_exams_code", columnNames = "code"))
public class ExamJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 20)
    private ExamAccessMode accessMode;

    @Embedded
    private ExamConfigJpaEmbeddable config;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "exam_questions",
            joinColumns = @JoinColumn(name = "exam_id"))
    @OrderColumn(name = "position")
    @Column(name = "question_id", nullable = false)
    private List<Long> questionIds = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "exam_generation_tags",
            joinColumns = @JoinColumn(name = "exam_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagJpaEntity> generationTags = new HashSet<>();

    @Column(length = 50)  // nullable; drafts no tienen
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public ExamJpaEntity() {
        // Requerido por JPA
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public ExamStatus getStatus() { return status; }
    public void setStatus(ExamStatus status) { this.status = status; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public ExamAccessMode getAccessMode() { return accessMode; }
    public void setAccessMode(ExamAccessMode accessMode) { this.accessMode = accessMode; }

    public ExamConfigJpaEmbeddable getConfig() { return config; }
    public void setConfig(ExamConfigJpaEmbeddable config) { this.config = config; }

    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }

    public Set<TagJpaEntity> getGenerationTags() { return generationTags; }
    public void setGenerationTags(Set<TagJpaEntity> generationTags) { this.generationTags = generationTags; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
