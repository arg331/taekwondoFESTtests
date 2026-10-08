package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.entity.Question;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/**
 * Filtros combinables para buscar preguntas.
 */
public final class QuestionSpecifications {

    private QuestionSpecifications() {}

    public static Specification<Question> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("ownerId"), ownerId);
    }

    /** Preguntas que tienen al menos uno de los tags indicados. */
    public static Specification<Question> hasAnyTag(Collection<Long> tagIds) {
        return (root, query, cb) -> {
            if (tagIds == null || tagIds.isEmpty()) return cb.conjunction();
            if (query != null) query.distinct(true);
            return root.join("tags").get("id").in(tagIds);
        };
    }

    public static Specification<Question> textContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("text")), "%" + text.trim().toLowerCase() + "%");
        };
    }
}
