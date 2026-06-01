package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.tag.dto.TagView;

import java.time.LocalDateTime;

/**
 * DTO HTTP de salida para un tag.
 *
 * Convertido desde TagView (aplicación) por el controller. Igual que
 * con CreateTagRequest, está separado para que la API REST pueda
 * evolucionar independientemente del modelo interno.
 */
public record TagResponse(
        Long id,
        String name,
        String color,
        LocalDateTime createdAt
) {
    public static TagResponse from(TagView view) {
        return new TagResponse(
                view.id(),
                view.name(),
                view.color(),
                view.createdAt()
        );
    }
}
