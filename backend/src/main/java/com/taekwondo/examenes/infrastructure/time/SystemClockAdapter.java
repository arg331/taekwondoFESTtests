package com.taekwondo.examenes.infrastructure.time;

import com.taekwondo.examenes.domain.port.Clock;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Implementación de producción del puerto Clock.
 * Devuelve el instante actual del reloj del sistema.
 *
 * Tests pueden sustituir este bean por uno determinista.
 */
@Component
public class SystemClockAdapter implements Clock {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
