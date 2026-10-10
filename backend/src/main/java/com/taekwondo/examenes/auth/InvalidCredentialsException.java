package com.taekwondo.examenes.auth;

/**
 * Credenciales incorrectas o sesión inválida (401).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
