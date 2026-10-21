package com.taekwondo.examenes.security;

import com.taekwondo.examenes.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad stateless con JWT.
 *
 * <ul>
 *   <li>Público: registro, login, lista de clubes, consultar un examen por código, empezarlo y entregarlo.</li>
 *   <li>Cualquier usuario con sesión: /api/auth/me, /api/results/me, /api/exams/public y elegir su club.</li>
 *   <li>Solo ADMIN: gestión de usuarios y roles.</li>
 *   <li>TEACHER o ADMIN: todo lo demás de preguntas, tags, exámenes, favoritos, resultados y clubes
 *       (quién gestiona cada club lo decide ClubService).</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String ADMIN = UserRole.ADMIN.name();
    private static final String TEACHER = UserRole.TEACHER.name();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/exams/by-code/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clubs").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/exams/by-code/*/attempts",
                                "/api/exams/by-code/*/attempts/resume").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/exams/by-code/*/attempts/answers").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/results").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/me/password", "/api/results/me",
                                "/api/users/me/club").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/exams/public").authenticated()
                        .requestMatchers("/api/users/**").hasAuthority(ADMIN)
                        .requestMatchers("/api/questions/**", "/api/tags/**", "/api/exams/**",
                                "/api/favorites/**", "/api/results/**", "/api/clubs/**", "/api/shares/**")
                                .hasAnyAuthority(ADMIN, TEACHER)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Cuánto esperar tras demasiados intentos (RNF-07)
        config.setExposedHeaders(List.of("Retry-After"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
