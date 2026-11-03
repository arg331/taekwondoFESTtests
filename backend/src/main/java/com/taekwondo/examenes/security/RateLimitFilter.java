package com.taekwondo.examenes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos por dirección (RNF-07) en el registro, la recuperación de la cuenta, la entrega, la portada
 * del examen, empezar o recuperar un intento y guardar respuestas (DE-25); el inicio de sesión va aparte
 * (LoginThrottle), porque allí solo cuentan los fallos. Cada dirección tiene un cupo por minuto; pasado, responde
 * 429 con el formato de error habitual y Retry-After hasta que empiece el minuto siguiente. Lo del examen tiene
 * cupos altos: un aula entera llega por la misma dirección de su router. El cupo es por regla y no por ruta:
 * pedir la portada de códigos distintos gasta el mismo, así que no se pueden recorrer los códigos.
 * Se apaga con app.rate-limit.enabled=false (los tests que no lo prueban).
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String TOO_MANY ="Demasiados intentos. Espera un minuto y vuelve a intentarlo.";
    private static final Duration WINDOW = Duration.ofMinutes(1);
    /** Por encima de tantas direcciones guardadas se olvidan las de minutos pasados. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final List<Rule> rules;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitFilter(@Value("${app.rate-limit.register-per-minute:30}") int registerPerMinute,
                           @Value("${app.rate-limit.submit-per-minute:300}") int submitPerMinute,
                           @Value("${app.rate-limit.recovery-per-minute:5}") int recoveryPerMinute,
                           @Value("${app.rate-limit.exam-page-per-minute:300}") int examPagePerMinute,
                           @Value("${app.rate-limit.attempt-start-per-minute:300}") int attemptStartPerMinute,
                           @Value("${app.rate-limit.answers-per-minute:3000}") int answersPerMinute,
                           Clock clock) {
        this.rules = List.of(
                new Rule("register", "POST", "/api/auth/register", registerPerMinute),
                new Rule("submit", "POST", "/api/results", submitPerMinute),
                // Recuperar la cuenta envía correos: sin límite, cualquiera llenaría el buzón de otro (RF-34)
                new Rule("recovery", "POST", "/api/auth/recovery", recoveryPerMinute),
                new Rule("recovery-reset", "POST", "/api/auth/recovery/reset", recoveryPerMinute),
                new Rule("exam-page", "GET", "/api/exams/by-code/{code}", examPagePerMinute),
                // Cada intento guarda sus preguntas: sin límite, con nombres inventados se llenaría la base de datos
                new Rule("attempt-start", "POST", "/api/exams/by-code/{code}/attempts", attemptStartPerMinute),
                new Rule("attempt-start", "POST", "/api/exams/by-code/{code}/attempts/resume", attemptStartPerMinute),
                // Una petición por pregunta contestada: 100 alumnos con 30 preguntas en un minuto
                new Rule("answers", "PUT", "/api/exams/by-code/{code}/attempts/answers", answersPerMinute));
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        PathContainer path = PathContainer.parsePath(decodedPath(request));
        Rule rule = rules.stream().filter(r -> r.matches(request.getMethod(), path)).findFirst().orElse(null);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        Instant now = clock.instant();
        Window window = windows.compute(rule.group() + "|" + request.getRemoteAddr(),
                (key, current) -> current == null || current.isOverAt(now) ? new Window(now, 1) : current.next());
        if (window.count() > rule.limit()) {
            reject(response, Duration.between(now, window.end()));
            return;
        }
        forgetOldWindows(now);
        chain.doFilter(request, response);
    }

    /**
     * Ruta ya descodificada, la misma con la que Spring elige el controlador. Con la ruta tal como llega
     * (getRequestURI), /api/auth/%72egister llegaba al registro sin pasar por el límite.
     */
    private static String decodedPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        return pathInfo == null ? request.getServletPath() : request.getServletPath() + pathInfo;
    }

    private void reject(HttpServletResponse response, Duration retryAfter) throws IOException {
        long seconds = Math.max(1, (retryAfter.toMillis() + 999) / 1000);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(seconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // El mismo formato que el resto de errores (RNF-05)
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now(clock) + "\",\"status\":429,"
                + "\"error\":\"Too Many Requests\",\"message\":\"" + TOO_MANY + "\"}");
    }

    private void forgetOldWindows(Instant now) {
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> w.isOverAt(now));
        }
    }

    /** Peticiones con ese método y ruta; las reglas del mismo grupo comparten cupo. */
    private record Rule(String group, String method, PathPattern pattern, int limit) {
        Rule(String group, String method, String pattern, int limit) {
            this(group, method, PathPatternParser.defaultInstance.parse(pattern), limit);
        }

        boolean matches(String requestMethod, PathContainer path) {
            return method.equals(requestMethod) && pattern.matches(path);
        }
    }

    /** Intentos de una dirección en el minuto que empezó en start. */
    private record Window(Instant start, int count) {
        Instant end() {
            return start.plus(WINDOW);
        }

        boolean isOverAt(Instant now) {
            return !now.isBefore(end());
        }

        Window next() {
            return new Window(start, count + 1);
        }
    }
}
