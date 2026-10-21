package com.taekwondo.examenes.question;

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
 * Pregunta tipo test de al menos 2 opciones. Pertenece al profesor que la crea.
 */
@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    public static final int MIN_OPTIONS = 2;
    /** Sin máximo para el profesor (RF-09); este tope solo evita peticiones desmesuradas. */
    public static final int MAX_OPTIONS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = 500)
    private List<String> options = new ArrayList<>();

    /** Índices de las opciones correctas dentro de {@link #options}: una o varias (RF-09). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_correct_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_index", nullable = false)
    private Set<Integer> correctAnswers = new HashSet<>();

    @Column(length = 2000)
    private String explanation;

    /** Cinturón mínimo (RF-43); opcional, null = sin grado. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Grade grade;

    /** C, D o ninguno (null). */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 10)
    private QuestionType type;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "question_tags",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Question(Long ownerId) {
        this.ownerId = ownerId;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String text, List<String> options, List<Integer> correctAnswers,
                       String explanation, Grade grade, QuestionType type, Set<Tag> tags) {
        this.text = text;
        this.options.clear();
        this.options.addAll(options);
        this.correctAnswers.clear();
        this.correctAnswers.addAll(correctAnswers);
        this.explanation = explanation;
        this.grade = grade;
        this.type = type;
        this.tags.clear();
        this.tags.addAll(tags);
    }

    /** Las correctas, de menor a mayor. */
    public List<Integer> getCorrectAnswers() {
        return correctAnswers.stream().sorted().toList();
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
