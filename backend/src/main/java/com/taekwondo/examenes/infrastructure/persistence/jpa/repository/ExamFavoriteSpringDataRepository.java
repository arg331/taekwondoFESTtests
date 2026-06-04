package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamFavoriteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data para ExamFavoriteJpaEntity.
 *
 * Métodos derivados del nombre (Spring Data genera el SQL):
 *  - findByProfessorIdAndExamId: lookup de un favorito concreto
 *  - existsByProfessorIdAndExamId: comprobación rápida sin cargar la entidad
 *  - findAllByProfessorId: listar todos los favoritos del profesor
 *  - deleteByProfessorIdAndExamId: borrar un favorito por la pareja
 *    (Spring Data lo genera; necesita @Transactional en el adapter
 *    o anotación a nivel de la interfaz, pero Spring Data ya lo
 *    decora internamente cuando se llama desde un Repository bean).
 */
public interface ExamFavoriteSpringDataRepository extends JpaRepository<ExamFavoriteJpaEntity, Long> {

    Optional<ExamFavoriteJpaEntity> findByProfessorIdAndExamId(Long professorId, Long examId);

    boolean existsByProfessorIdAndExamId(Long professorId, Long examId);

    List<ExamFavoriteJpaEntity> findAllByProfessorId(Long professorId);

    void deleteByProfessorIdAndExamId(Long professorId, Long examId);
}
