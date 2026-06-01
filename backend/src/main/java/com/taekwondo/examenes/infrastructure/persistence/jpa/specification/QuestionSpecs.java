package com.taekwondo.examenes.infrastructure.persistence.jpa.specification;

import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.QuestionJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filtros componibles para QuestionJpaEntity.
 *
 * Cada método estático devuelve una Specification que representa UN filtro.
 * Las Specifications se combinan con Specification.allOf(...) en el adaptador.
 *
 * Filtros que devuelven null se ignoran automáticamente al combinarse.
 * Esto permite tener filtros opcionales sin un if dentro del adaptador.
 *
 * Patrón Specification (Evans, DDD). Cada filtro encapsulado y reutilizable.
 * Añadir un filtro nuevo = añadir un método aquí, sin tocar los existentes
 * (OCP de SOLID).
 */
public final class QuestionSpecs {

    private QuestionSpecs() {}

    /**
     * Pregunta perteneciente al profesor indicado.
     * Obligatorio en toda búsqueda: cada profesor solo ve sus preguntas.
     */
    public static Specification<QuestionJpaEntity> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    /**
     * Pregunta que tiene AL MENOS UNO de los tags indicados.
     * Si el set es null o vacío, devuelve null y el filtro se ignora.
     */
    public static Specification<QuestionJpaEntity> hasAnyOfTags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) return null;

        Set<Long> tagIds = tags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        return (root, query, cb) -> {
            // distinct para evitar duplicados al hacer join con la tabla intermedia
            if (query != null) query.distinct(true);
            Join<QuestionJpaEntity, TagJpaEntity> tagJoin = root.join("tags");
            return tagJoin.get("id").in(tagIds);
        };
    }

    /**
     * Pregunta cuyo texto contiene el patrón dado (case-insensitive).
     * Si el patrón es null o vacío, devuelve null y el filtro se ignora.
     */
    public static Specification<QuestionJpaEntity> textContains(String pattern) {
        if (pattern == null || pattern.isBlank()) return null;
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("text")),
                "%" + pattern.toLowerCase() + "%"
        );
    }
}
