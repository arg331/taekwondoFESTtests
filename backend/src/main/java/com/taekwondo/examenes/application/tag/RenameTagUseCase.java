package com.taekwondo.examenes.application.tag;

import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.application.tag.dto.RenameTagInput;
import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.TagRepository;

/**
 * Caso de uso: renombrar un tag existente.
 *
 * Pasos:
 *  1. Buscar el tag por ID.
 *  2. Verificar que pertenece al profesor solicitante.
 *  3. Si el nombre cambia, verificar que no choca con otro tag del mismo profesor.
 *  4. Aplicar el cambio en el dominio (que valida formato del nombre).
 *  5. Persistir y devolver la vista.
 */
public class RenameTagUseCase {

    private final TagRepository tagRepository;

    public RenameTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagView execute(RenameTagInput input) {
        // 1. Buscar el tag
        Tag tag = tagRepository.findById(input.tagId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un tag con id " + input.tagId()));

        // 2. Verificar pertenencia
        if (!tag.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un tag con id " + input.tagId());
        }

        // 3. Si el nombre realmente cambia, verificar que no choca con otro tag
        boolean nameIsChanging = !tag.getName().equals(input.newName());
        if (nameIsChanging
                && tagRepository.existsByNameAndOwnerId(input.newName(), input.requesterOwnerId())) {
            throw new BusinessRuleViolationException(
                    "Ya existe un tag con el nombre '" + input.newName() + "'");
        }

        // 4. Aplicar la operación de negocio (la entidad valida el formato del nombre)
        tag.rename(input.newName());

        // 5. Persistir y devolver vista
        Tag saved = tagRepository.save(tag);
        return TagView.from(saved);
    }
}