package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.QuestionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * Repositorio Spring Data JPA para QuestionJpaEntity.
 *
 * Extiende DOS interfaces:
 *  - JpaRepository: operaciones CRUD básicas y consultas derivadas
 *  - JpaSpecificationExecutor: consultas dinámicas mediante Specification
 *
 * El puente entre esta interfaz y el puerto QuestionRepository del dominio
 * lo hace QuestionRepositoryJpaAdapter.
 */
public interface QuestionSpringDataRepository
        extends JpaRepository<QuestionJpaEntity, Long>,
                JpaSpecificationExecutor<QuestionJpaEntity> {

    List<QuestionJpaEntity> findAllByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);
}
