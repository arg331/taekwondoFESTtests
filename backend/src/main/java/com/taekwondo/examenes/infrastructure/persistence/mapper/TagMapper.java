package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;

/**
 * Conversor entre Tag (dominio) y TagJpaEntity (persistencia).
 *
 * Centraliza el mapeo en un único punto. Si Tag o TagJpaEntity cambian,
 * solo este archivo necesita actualizarse.
 *
 * Clase utilitaria con métodos estáticos: no tiene estado y no necesita
 * inyección.
 */
public final class TagMapper {

    private TagMapper() {}    // utilitaria, no se instancia

    public static TagJpaEntity toJpa(Tag tag) {
        return new TagJpaEntity(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getOwnerId(),
                tag.getCreatedAt()
        );
    }

    public static Tag toDomain(TagJpaEntity entity) {
        return Tag.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getColor(),
                entity.getOwnerId(),
                entity.getCreatedAt()
        );
    }
}
