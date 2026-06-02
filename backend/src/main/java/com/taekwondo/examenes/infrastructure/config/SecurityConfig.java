package com.taekwondo.examenes.infrastructure.config;

import com.taekwondo.examenes.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security con JWT.
 *
 * Endpoints públicos:
 *  - POST /api/auth/register
 *  - POST /api/auth/login
 *  - GET  /api/exams/by-code/**   (estudiante anónimo entra con el código del QR)
 *  - /h2-console/** (DEV ONLY)
 *  - /error
 *
 * Todo lo demás requiere autenticación válida (JWT).
 *
 * Cuando llegue Result end-to-end, POST /api/results también será público
 * (un estudiante anónimo puede enviar respuestas si el examen es OPEN).
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/api/auth/register",
                                          "/api/auth/login",
                                          "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/exams/by-code/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Resto requiere autenticación
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
