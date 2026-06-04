package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;

import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de usuarios.
 *
 * Sirve para ambos roles (ADMIN y STUDENT). El filtrado por rol queda
 * a discreción de los casos de uso si lo necesitan.
 *
 * NO incluimos deleteById ni findAll: los usuarios no se borran
 * (se desactivan: User.deactivate()).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Indica si existe al menos un usuario con el rol dado.
     * Usado por el seed para comprobar si ya existe un ADMIN
     * sin depender de un username concreto.
     */
    boolean existsByRole(UserRole role);
}
