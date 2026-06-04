package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para TagJpaEntity.
 *
 * Esta interfaz NO implementa TagRepository (el puerto del dominio).
 * Es una interfaz de Spring Data: Spring genera la implementación
 * automáticamente en runtime, derivando consultas SQL de los nombres
 * de los métodos.
 *
 * El puente entre esta interfaz y el puerto TagRepository del dominio
 * lo hace TagRepositoryJpaAdapter.
 */
public interface TagSpringDataRepository extends JpaRepository<TagJpaEntity, Long> {

    List<TagJpaEntity> findAllByOwnerId(Long ownerId);

    boolean existsByNameAndOwnerId(String name, Long ownerId);
}
