package com.taekwondo.examenes.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Value object: criterios de búsqueda de preguntas.
 *
 * Inmutable. Se construye con Builder porque tiene varios campos opcionales,
 * y un constructor con 5 parámetros sería ilegible.
 *
 * Diseño OCP: añadir nuevos filtros (ej: difficulty) se hace ampliando este
 * objeto y su builder, sin tocar la firma del puerto QuestionRepository.
 */
public final class QuestionCriteria {

    private final Long ownerId;
    private final Set<Tag> anyOfTags;
    private final String textContains;

    private QuestionCriteria(Builder b) {
        this.ownerId = Objects.requireNonNull(b.ownerId, "ownerId es obligatorio");
        this.anyOfTags = Collections.unmodifiableSet(new HashSet<>(b.anyOfTags));
        this.textContains = b.textContains;
    }

    public Long getOwnerId()         { return ownerId; }
    public Set<Tag> getAnyOfTags()   { return anyOfTags; }
    public String getTextContains()  { return textContains; }

    public boolean hasTagFilter()    { return !anyOfTags.isEmpty(); }
    public boolean hasTextFilter()   { return textContains != null && !textContains.isBlank(); }

    public static Builder forOwner(Long ownerId) {
        return new Builder(ownerId);
    }

    public static final class Builder {
        private final Long ownerId;
        private Set<Tag> anyOfTags = new HashSet<>();
        private String textContains;

        private Builder(Long ownerId) {
            this.ownerId = ownerId;
        }

        public Builder withAnyOfTags(Set<Tag> tags) {
            this.anyOfTags = (tags != null) ? new HashSet<>(tags) : new HashSet<>();
            return this;
        }

        public Builder withTextContains(String text) {
            this.textContains = text;
            return this;
        }

        public QuestionCriteria build() {
            return new QuestionCriteria(this);
        }
    }
}