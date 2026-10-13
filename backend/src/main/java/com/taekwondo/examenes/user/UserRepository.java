package com.taekwondo.examenes.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    /** Miembros de un club, por nombre visible. */
    @Query("select u from User u where u.club.id = :clubId order by lower(u.displayName)")
    List<User> findAllByClubId(Long clubId);
}
