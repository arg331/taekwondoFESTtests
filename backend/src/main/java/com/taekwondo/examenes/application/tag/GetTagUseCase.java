package com.taekwondo.examenes.application.tag;

import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.TagRepository;

/**
 * Caso de uso: obtener un tag por su ID, comprobando que pertenece
 * al profesor que lo solicita.
 *
 * La comprobación de pertenencia (ownerId) es CRÍTICA: sin ella, un profesor
 * podría leer tags de otros profesores conociendo el ID. Esto es seguridad
 * a nivel de aplicación, no de framework.
 */
public class GetTagUseCase {

    private final TagRepository tagRepository;

    public GetTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagView execute(Long tagId, Long requesterOwnerId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un tag con id " + tagId));

        // Seguridad a nivel de dominio: un profesor solo puede acceder a sus tags.
        // Si el tag no le pertenece, hacemos como si no existiera (no revelamos
        // su existencia a quien no debería verlo).
        if (!tag.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un tag con id " + tagId);
        }

        return TagView.from(tag);
    }
}