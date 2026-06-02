package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.domain.model.ExamStatus;
import com.taekwondo.examenes.domain.model.Visibility;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data para ExamJpaEntity.
 *
 * Métodos derivados del nombre — Spring Data los genera automáticamente:
 *  - findByCode: lookup por código de acceso (índice UNIQUE)
 *  - findAllByOwnerId: listar todos los exámenes de un profesor
 *  - findAllByVisibilityAndStatus: usado por el adapter para findAllPublic()
 *  - existsByCode: comprobación de colisión al generar códigos
 *
 * Solo expone lo que el adapter realmente necesita (ISP).
 */
public interface ExamSpringDataRepository extends JpaRepository<ExamJpaEntity, Long> {

    Optional<ExamJpaEntity> findByCode(String code);

    List<ExamJpaEntity> findAllByOwnerId(Long ownerId);

    List<ExamJpaEntity> findAllByVisibilityAndStatus(Visibility visibility, ExamStatus status);

    boolean existsByCode(String code);
}
