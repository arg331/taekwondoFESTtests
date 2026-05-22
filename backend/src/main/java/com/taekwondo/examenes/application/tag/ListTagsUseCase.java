package com.taekwondo.examenes.application.tag;

import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.port.TagRepository;

import java.util.List;

/**
 * Caso de uso: listar todos los tags de un profesor.
 *
 * No requiere comprobaciones adicionales: cada profesor solo ve sus tags
 * (filtrado por ownerId en el repositorio).
 */
public class ListTagsUseCase {

    private final TagRepository tagRepository;

    public ListTagsUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagView> execute(Long ownerId) {
        return tagRepository.findAllByOwnerId(ownerId).stream()
                .map(TagView::from)
                .toList();
    }
}