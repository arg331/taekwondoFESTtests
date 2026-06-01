package com.taekwondo.examenes.application.shared.exception;

/**
 * Lanzada cuando un intento de login falla.
 *
 * Mensaje genérico ("credenciales inválidas") sin distinguir entre
 * "usuario no existe" y "contraseña incorrecta" para evitar enumeración
 * de usuarios.
 *
 * El controller traducirá esta excepción a HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
