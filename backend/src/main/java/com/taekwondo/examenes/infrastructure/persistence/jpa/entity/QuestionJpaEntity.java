package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import com.taekwondo.examenes.domain.model.Difficulty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entidad de persistencia para Question. NO es la entidad de dominio.
 *
 * Mapea a tres tablas:
 *  - questions          (fila principal)
 *  - question_options   (lista ordenada de 4 opciones, @ElementCollection)
 *  - question_tags      (tabla intermedia ManyToMany con tags)
 *
 * Las conversiones Question <-> QuestionJpaEntity las hace QuestionMapper,
 * que a su vez delega en TagMapper para los tags.
 */
@Entity
@Table(name = "questions")
public class QuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    /**
     * Lista de opciones de la pregunta. JPA la guarda en una tabla auxiliar
     * 'question_options' con la columna 'option_order' para preservar el orden.
     * Hibernate gestiona inserción/borrado automáticamente.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "question_options",
            joinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = 500)
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    @Column(length = 2000)
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * Relación ManyToMany con tags. EAGER para evitar LazyInitializationException
     * cuando se devuelve la pregunta fuera de la sesión Hibernate.
     *
     * Se usa Set (no List) para evitar el problema MultipleBagFetchException
     * si en el futuro se añadiera otra colección EAGER.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "question_tags",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagJpaEntity> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA exige constructor sin args
    protected QuestionJpaEntity() {}

    public QuestionJpaEntity(Long id, String text, List<String> options, int correctAnswer,
                              String explanation, Difficulty difficulty, Long ownerId,
                              Set<TagJpaEntity> tags,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.text = text;
        this.options = new ArrayList<>(options);
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.ownerId = ownerId;
        this.tags = new HashSet<>(tags);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId()                  { return id; }
    public String getText()              { return text; }
    public List<String> getOptions()     { return options; }
    public int getCorrectAnswer()        { return correctAnswer; }
    public String getExplanation()       { return explanation; }
    public Difficulty getDifficulty()    { return difficulty; }
    public Long getOwnerId()             { return ownerId; }
    public Set<TagJpaEntity> getTags()   { return tags; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    public void setId(Long id)                              { this.id = id; }
    public void setText(String text)                        { this.text = text; }
    public void setOptions(List<String> options)            { this.options = options; }
    public void setCorrectAnswer(int correctAnswer)         { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation)          { this.explanation = explanation; }
    public void setDifficulty(Difficulty difficulty)        { this.difficulty = difficulty; }
    public void setOwnerId(Long ownerId)                    { this.ownerId = ownerId; }
    public void setTags(Set<TagJpaEntity> tags)             { this.tags = tags; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)       { this.updatedAt = updatedAt; }
}
