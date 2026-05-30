package com.taekwondo.examenes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad básica
 * Por ahora deshabilitamos Spring Security para desarrollo
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean para encriptar contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuración de seguridad HTTP
     * Por ahora permitimos todo (sin autenticación)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitamos CSRF para desarrollo
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Permitir todas las peticiones sin autenticación
            );
        
        return http.build();
    }
}