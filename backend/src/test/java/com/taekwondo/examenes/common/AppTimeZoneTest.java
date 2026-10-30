package com.taekwondo.examenes.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DE-09: las fechas se interpretan en la hora de España aunque el servidor esté en otra zona. */
@DisplayName("Zona horaria de la aplicación")
class AppTimeZoneTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restore() {
        TimeZone.setDefault(original);
    }

    @Test
    @DisplayName("Sin configurar, la hora de España aunque el servidor esté en UTC")
    void defaultIsMadrid() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        AppTimeZone.apply(null);

        assertThat(TimeZone.getDefault().getID()).isEqualTo("Europe/Madrid");
    }

    @Test
    @DisplayName("APP_TIME_ZONE la cambia, p. ej. para Canarias")
    void configured() {
        AppTimeZone.apply("Atlantic/Canary");

        assertThat(TimeZone.getDefault().getID()).isEqualTo("Atlantic/Canary");
    }

    @Test
    @DisplayName("Una zona que no existe no deja arrancar")
    void invalidZoneFails() {
        assertThatThrownBy(() -> AppTimeZone.apply("Nada/Nada")).isInstanceOf(DateTimeException.class);
    }
}
