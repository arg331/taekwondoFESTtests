package com.taekwondo.examenes.application.shared.exception;

/**
 * Lanzada cuando un caso de uso necesita un recurso que no existe.
 *
 * Ejemplo: pedir el tag con ID 42, pero no hay ningún tag con ese ID.
 *
 * El controller traducirá esto a HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}