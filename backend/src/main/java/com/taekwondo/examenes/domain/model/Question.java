package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Entidad de dominio: Question (pregunta de examen).
 *
 * REGLAS DE NEGOCIO:
 *  - Texto no vacío
 *  - Exactamente 4 opciones, ninguna vacía
 *  - La respuesta correcta es un índice entre 0 y 3
 *  - Tiene una dificultad
 *  - Pertenece a un profesor (ownerId)
 *  - Puede tener 0 o N tags
 *
 * Las preguntas son MUTABLES (el profesor las edita), pero los exámenes
 * guardan snapshots inmutables de las preguntas que tenían al publicarse.
 */
public final class Question {

    private static final int REQUIRED_OPTIONS = 4;

    private final Long id;
    private String text;
    private List<String> options;
    private int correctAnswer;
    private String explanation;
    private Difficulty difficulty;
    private final Long ownerId;
    private final Set<Tag> tags;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ──────────────────────────────────────────────────
    // Factory methods
    // ──────────────────────────────────────────────────

    public static Question createNew(String text,
                                      List<String> options,
                                      int correctAnswer,
                                      String explanation,
                                      Difficulty difficulty,
                                      Long ownerId) {
        validateText(text);
        validateOptions(options);
        validateCorrectAnswer(correctAnswer);
        validateDifficulty(difficulty);
        validateOwner(ownerId);

        LocalDateTime now = LocalDateTime.now();
        return new Question(
                null,
                text,
                new ArrayList<>(options),
                correctAnswer,
                explanation,
                difficulty,
                ownerId,
                new HashSet<>(),
                now,
                now
        );
    }

    public static Question reconstitute(Long id,
                                         String text,
                                         List<String> options,
                                         int correctAnswer,
                                         String explanation,
                                         Difficulty difficulty,
                                         Long ownerId,
                                         Set<Tag> tags,
                                         LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new Question(
                id,
                text,
                new ArrayList<>(options),
                correctAnswer,
                explanation,
                difficulty,
                ownerId,
                new HashSet<>(tags),
                createdAt,
                updatedAt
        );
    }

    private Question(Long id,
                     String text,
                     List<String> options,
                     int correctAnswer,
                     String explanation,
                     Difficulty difficulty,
                     Long ownerId,
                     Set<Tag> tags,
                     LocalDateTime createdAt,
                     LocalDateTime updatedAt) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.ownerId = ownerId;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ──────────────────────────────────────────────────
    // Operaciones de negocio
    // ──────────────────────────────────────────────────

    public void editText(String newText) {
        validateText(newText);
        this.text = newText;
        touch();
    }

    public void editOptions(List<String> newOptions, int newCorrectAnswer) {
        validateOptions(newOptions);
        validateCorrectAnswer(newCorrectAnswer);
        this.options = new ArrayList<>(newOptions);
        this.correctAnswer = newCorrectAnswer;
        touch();
    }

    public void editExplanation(String newExplanation) {
        this.explanation = newExplanation;
        touch();
    }

    public void changeDifficulty(Difficulty newDifficulty) {
        validateDifficulty(newDifficulty);
        this.difficulty = newDifficulty;
        touch();
    }

    public void addTag(Tag tag) {
        Objects.requireNonNull(tag, "tag no puede ser null");
        if (!Objects.equals(tag.getOwnerId(), this.ownerId)) {
            throw new IllegalArgumentException(
                    "No se puede añadir un tag de otro profesor a esta pregunta");
        }
        this.tags.add(tag);
        touch();
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        touch();
    }

    /**
     * Comprueba si la pregunta tiene al menos uno de los tags dados.
     * Útil para el filtro de pre-generación de exámenes.
     */
    public boolean hasAnyTag(Set<Tag> candidateTags) {
        if (candidateTags == null || candidateTags.isEmpty()) return false;
        for (Tag t : candidateTags) {
            if (this.tags.contains(t)) return true;
        }
        return false;
    }

    /**
     * Comprueba si la respuesta dada por un estudiante es correcta.
     */
    public boolean isAnswerCorrect(int givenAnswer) {
        return givenAnswer == this.correctAnswer;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ──────────────────────────────────────────────────
    // Validaciones
    // ──────────────────────────────────────────────────

    private static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("El texto de la pregunta no puede estar vacío");
        }
    }

    private static void validateOptions(List<String> options) {
        if (options == null || options.size() != REQUIRED_OPTIONS) {
            throw new IllegalArgumentException(
                    "La pregunta debe tener exactamente " + REQUIRED_OPTIONS + " opciones");
        }
        for (String opt : options) {
            if (opt == null || opt.isBlank()) {
                throw new IllegalArgumentException("Ninguna opción puede estar vacía");
            }
        }
    }

    private static void validateCorrectAnswer(int correctAnswer) {
        if (correctAnswer < 0 || correctAnswer >= REQUIRED_OPTIONS) {
            throw new IllegalArgumentException(
                    "La respuesta correcta debe estar entre 0 y " + (REQUIRED_OPTIONS - 1));
        }
    }

    private static void validateDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("La dificultad no puede ser null");
        }
    }

    private static void validateOwner(Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("La pregunta debe tener un propietario");
        }
    }

    // ──────────────────────────────────────────────────
    // Getters (sin setters: los cambios pasan por métodos de negocio)
    // ──────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getText()              { return text; }
    public List<String> getOptions()     { return Collections.unmodifiableList(options); }
    public int getCorrectAnswer()        { return correctAnswer; }
    public String getExplanation()       { return explanation; }
    public Difficulty getDifficulty()    { return difficulty; }
    public Long getOwnerId()             { return ownerId; }
    public Set<Tag> getTags()            { return Collections.unmodifiableSet(tags); }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // ──────────────────────────────────────────────────
    // Equals/hashCode por identidad
    // ──────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question q)) return false;
        return Objects.equals(id, q.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}