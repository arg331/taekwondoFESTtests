package com.taekwondo.examenes.domain.port;

import java.time.LocalDateTime;

/**
 * Puerto de salida: proveedor del tiempo actual.
 *
 * Existe para que los casos de uso NO llamen directamente a
 * LocalDateTime.now(). Inyectando este puerto, los tests pueden
 * controlar la fecha actual y verificar comportamientos dependientes
 * del tiempo (expiración, cálculos de fechas, etc.) de forma determinista.
 *
 * Implementación de producción: lee del reloj del sistema.
 * Implementación de tests: devuelve una fecha fija o controlable.
 */
public interface Clock {

    /**
     * @return el instante actual según la implementación.
     */
    LocalDateTime now();
}