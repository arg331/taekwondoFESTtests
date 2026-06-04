package com.taekwondo.examenes.domain.port;

/**
 * Puerto de salida: hashing y verificación de contraseñas.
 *
 * El dominio no conoce BCrypt, Argon2, ni ninguna librería específica.
 * En producción la implementación usará BCrypt (estándar de Spring Security).
 */
public interface PasswordHasher {

    /**
     * Genera el hash de una contraseña en claro.
     */
    String hash(String plainPassword);

    /**
     * Verifica si una contraseña en claro corresponde a un hash dado.
     */
    boolean matches(String plainPassword, String hash);
}
