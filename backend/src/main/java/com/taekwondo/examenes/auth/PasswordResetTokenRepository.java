package com.taekwondo.examenes.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Al pedir otro enlace, o al usarlo, los anteriores dejan de valer. */
    void deleteAllByUserId(Long userId);
}
