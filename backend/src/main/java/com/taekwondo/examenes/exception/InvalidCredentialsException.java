package com.taekwondo.examenes.exception;

/**
 * Credenciales incorrectas o sesión inválida (401).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
