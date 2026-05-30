package com.taekwondo.examenes.application.tag.dto;

import com.taekwondo.examenes.domain.model.Tag;

import java.time.LocalDateTime;

/**
 * Vista de un tag para devolver desde la capa de aplicación.
 *
 * Se reutilizará en múltiples casos de uso (create, rename, list, get).
 *
 * El método estático from() centraliza la conversión Tag → TagView:
 * cualquier caso de uso que devuelva tags usa el mismo mapeo, evitando
 * duplicación.
 */
public record TagView(
        Long id,
        String name,
        String color,
        LocalDateTime createdAt
) {
    public static TagView from(Tag tag) {
        return new TagView(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getCreatedAt()
        );
    }
}