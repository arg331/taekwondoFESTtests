package com.taekwondo.examenes.common;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * DE-09: las fechas de la aplicación no llevan zona (LocalDateTime) y el navegador manda la caducidad en hora
 * local. Si el servidor estuviera en UTC, la caducidad elegida en España se desplazaría 1 o 2 horas. Por eso, al
 * arrancar, la zona de la aplicación es la de España, o la de APP_TIME_ZONE (p. ej. Atlantic/Canary).
 */
public final class AppTimeZone {

    public static final String DEFAULT_ZONE = "Europe/Madrid";

    private AppTimeZone() {}

    /** Antes de arrancar Spring: así la usan el reloj inyectable, Hibernate y los LocalDateTime.now(). */
    public static void apply(String configured) {
        String zone = configured == null || configured.isBlank() ? DEFAULT_ZONE : configured.trim();
        // ZoneId.of falla con una zona que no existe: mejor no arrancar que fechar mal
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zone)));
    }
}
