package com.taekwondo.examenes.application.tag;

import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.tag.dto.CreateTagInput;
import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.TagRepository;

/**
 * Caso de uso: crear un tag nuevo para un profesor.
 *
 * Responsabilidad ÚNICA: orquestar la creación de un tag.
 *
 * Pasos:
 *  1. Comprobar que no existe ya un tag con ese nombre para el profesor
 *     (regla que requiere consultar el repositorio).
 *  2. Construir la entidad Tag (las validaciones de formato las hace ella misma).
 *  3. Persistir.
 *  4. Devolver una vista.
 *
 * NO sabe nada de:
 *  - HTTP (la traducción REST la hace el controller).
 *  - JPA (la persistencia la hace la implementación de TagRepository).
 *  - Spring (no usa @Service ni @Autowired).
 */
public class CreateTagUseCase {

    private final TagRepository tagRepository;

    public CreateTagUseCase(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagView execute(CreateTagInput input) {
        // 1. Regla de coordinación: no puede haber dos tags con el mismo nombre
        //    para el mismo profesor. Necesita consultar el repositorio,
        //    por eso no puede estar en la entidad Tag.
        if (tagRepository.existsByNameAndOwnerId(input.name(), input.ownerId())) {
            throw new BusinessRuleViolationException(
                    "Ya existe un tag con el nombre '" + input.name() + "'");
        }

        // 2. Construir la entidad. Las validaciones de formato (nombre vacío,
        //    color hex inválido, owner null) las dispara la propia entidad.
        Tag tag = Tag.createNew(input.name(), input.color(), input.ownerId());

        // 3. Persistir mediante el puerto. El caso de uso ni sabe ni le importa
        //    si por debajo es JPA, MongoDB o un Map en memoria (DIP).
        Tag persisted = tagRepository.save(tag);

        // 4. Devolver vista. Nunca exponemos la entidad de dominio fuera de
        //    la capa de aplicación.
        return TagView.from(persisted);
    }
}