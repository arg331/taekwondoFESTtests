package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.ExamFavorite;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamFavoriteJpaEntity;

/**
 * Conversión bidireccional entre ExamFavorite (dominio) y
 * ExamFavoriteJpaEntity (JPA).
 *
 * Mapper trivial: la entidad es plana, sin relaciones externas.
 */
public final class ExamFavoriteMapper {

    private ExamFavoriteMapper() {}

    public static ExamFavorite toDomain(ExamFavoriteJpaEntity entity) {
        return ExamFavorite.reconstitute(
                entity.getId(),
                entity.getProfessorId(),
                entity.getExamId(),
                entity.getCreatedAt()
        );
    }

    public static ExamFavoriteJpaEntity toJpa(ExamFavorite favorite) {
        ExamFavoriteJpaEntity entity = new ExamFavoriteJpaEntity();
        entity.setId(favorite.getId());
        entity.setProfessorId(favorite.getProfessorId());
        entity.setExamId(favorite.getExamId());
        entity.setCreatedAt(favorite.getCreatedAt());
        return entity;
    }
}
