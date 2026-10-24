package com.taekwondo.examenes.common;

/**
 * El recurso no existe o no pertenece al usuario que lo pide (404).
 * Se usa el mismo error en ambos casos para no revelar recursos ajenos.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
