package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data para ResultJpaEntity.
 *
 * Métodos derivados del nombre (Spring Data genera el SQL):
 *  - findAllByExamId: listar resultados de un examen concreto
 *  - findAllByStudentUserId: listar resultados de un estudiante registrado
 *  - existsByExamIdAndStudentName: comprobar duplicados por nombre
 *  - countByExamId: total de intentos de un examen (para estadísticas)
 *
 * findById / save vienen de JpaRepository.
 * No exponemos delete: Result es inmutable e histórico (decisión del puerto).
 */
public interface ResultSpringDataRepository extends JpaRepository<ResultJpaEntity, Long> {

    List<ResultJpaEntity> findAllByExamId(Long examId);

    List<ResultJpaEntity> findAllByStudentUserId(Long studentUserId);

    boolean existsByExamIdAndStudentName(Long examId, String studentName);

    long countByExamId(Long examId);
}
