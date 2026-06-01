package com.taekwondo.examenes.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración minimal de Spring Security para DESARROLLO.
 *
 * Permite TODO sin autenticación. Útil para iterar end-to-end con
 * Postman/curl mientras no tengamos JWT funcional.
 *
 * IMPORTANTE: esta clase NO sirve para producción. Cuando integremos
 * autenticación de verdad (JWT + filtros), esta clase se reemplazará
 * por una versión que:
 *  - Requiere token JWT en endpoints protegidos.
 *  - Distingue endpoints públicos (/api/auth/**, /h2-console)
 *    de los privados.
 *  - Procesa el token en un filtro custom.
 *
 * Decisiones temporales tomadas aquí:
 *  - CSRF deshabilitado: imprescindible para APIs REST que reciben
 *    POST/PATCH/DELETE sin sesión web (no hay token CSRF).
 *  - Sesión STATELESS: el servidor no guarda sesión; cada petición
 *    es independiente. Encaja con el modelo JWT futuro.
 *  - frameOptions deshabilitado: necesario para que la consola H2
 *    funcione en /h2-console (la consola se sirve en un iframe).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
