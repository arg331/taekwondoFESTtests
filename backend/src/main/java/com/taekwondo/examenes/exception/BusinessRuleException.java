package com.taekwondo.examenes.exception;

/**
 * La petición es válida pero choca con una regla de negocio
 * o con el estado actual del recurso (409).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
