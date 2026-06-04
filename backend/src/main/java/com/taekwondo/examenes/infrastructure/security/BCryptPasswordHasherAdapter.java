package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.domain.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto PasswordHasher usando el PasswordEncoder
 * de Spring Security (BCrypt por defecto).
 *
 * El puerto del dominio no conoce BCrypt; este adapter encapsula
 * la dependencia con Spring Security.
 *
 * BCrypt es resistente a fuerza bruta porque cada hash usa un salt
 * único y el coste computacional es ajustable (Spring usa 10 por
 * defecto, suficiente para 2026).
 */
@Component
public class BCryptPasswordHasherAdapter implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHasherAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String hash) {
        if (plainPassword == null || hash == null) return false;
        return passwordEncoder.matches(plainPassword, hash);
    }
}
