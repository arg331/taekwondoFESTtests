package com.taekwondo.examenes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable: los servicios usan LocalDateTime.now(clock) para que los
 * tests puedan fijar la hora (expiración de exámenes, tiempo límite).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
