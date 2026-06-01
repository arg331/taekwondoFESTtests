package com.taekwondo.examenes.infrastructure.persistence.jpa.repository;

import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para UserJpaEntity.
 *
 * Spring genera las implementaciones automáticamente desde los nombres
 * de los métodos.
 */
public interface UserSpringDataRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
