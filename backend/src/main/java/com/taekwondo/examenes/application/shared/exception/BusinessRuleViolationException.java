package com.taekwondo.examenes.application.shared.exception;

/**
 * Lanzada cuando una operación viola una regla de negocio detectada
 * en la capa de aplicación (no en el dominio puro).
 *
 * Ejemplo: intentar crear un tag con un nombre que ya está en uso
 * por el mismo profesor. Esta regla solo se puede comprobar consultando
 * el repositorio, por eso vive aquí y no en la entidad Tag.
 *
 * Más adelante, el controller (capa de infraestructura) traducirá esta
 * excepción a una respuesta HTTP adecuada (típicamente 409 Conflict).
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}