package com.taekwondo.examenes.common;

import java.time.Duration;

/**
 * Demasiados intentos (429): cuánto hay que esperar va en la cabecera Retry-After (RNF-07).
 */
public class TooManyAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyAttemptsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    /** Segundos enteros que esperar, al menos 1. */
    public long retryAfterSeconds() {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
