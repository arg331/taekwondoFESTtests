package com.taekwondo.examenes.common;

/**
 * El usuario tiene sesión, pero no permiso sobre ese recurso concreto (403).
 * Se usa cuando el permiso depende del recurso (por ejemplo, ser profesor de un club),
 * algo que la configuración de seguridad por rutas no puede decidir.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
