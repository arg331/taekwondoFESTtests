package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones de base de datos con User
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Buscar usuario por username
     */
    Optional<User> findByUsername(String username);

    /**
     * Buscar usuario por email
     */
    Optional<User> findByEmail(String email);

    /**
     * Verificar si existe un usuario con ese username
     */
    boolean existsByUsername(String username);

    /**
     * Verificar si existe un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Buscar usuario por username y que esté activo
     */
    Optional<User> findByUsernameAndActiveTrue(String username);
}
