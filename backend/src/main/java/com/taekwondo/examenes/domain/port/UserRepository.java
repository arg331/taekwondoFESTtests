package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.User;
import com.taekwondo.examenes.domain.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    List<User> findAll();
}
