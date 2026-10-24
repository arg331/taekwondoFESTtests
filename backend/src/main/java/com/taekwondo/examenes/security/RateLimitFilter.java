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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos por dirección (RNF-07) en el inicio de sesión, el registro, la recuperación de la
 * cuenta y la entrega de exámenes. Cada dirección tiene un cupo por minuto; pasado, responde 429 con el formato de error
 * habitual y Retry-After hasta que empiece el minuto siguiente. La entrega tiene un cupo alto: un aula
 * entera llega por la misma dirección de su router.
 * Se apaga con app.rate-limit.enabled=false (los tests que no lo prueban).
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    static final String TOO_MANY = "Demasiados intentos. Espera un minuto y vuelve a intentarlo.";
    private static final Duration WINDOW = Duration.ofMinutes(1);
    /** Por encima de tantas direcciones guardadas se olvidan las de minutos pasados. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    /** Ruta (POST) → intentos por minuto y dirección. */
    private final Map<String, Integer> limits;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitFilter(@Value("${app.rate-limit.login-per-minute:10}") int loginPerMinute,
                           @Value("${app.rate-limit.register-per-minute:5}") int registerPerMinute,
                           @Value("${app.rate-limit.submit-per-minute:300}") int submitPerMinute,
                           @Value("${app.rate-limit.recovery-per-minute:5}") int recoveryPerMinute,
                           Clock clock) {
        this.limits = Map.of(
                "/api/auth/login", loginPerMinute,
                "/api/auth/register", registerPerMinute,
                "/api/results", submitPerMinute,
                // Recuperar la cuenta envía correos: sin límite, cualquiera llenaría el buzón de otro (RF-34)
                "/api/auth/recovery", recoveryPerMinute,
                "/api/auth/recovery/reset", recoveryPerMinute);
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Integer limit = "POST".equals(request.getMethod()) ? limits.get(request.getRequestURI()) : null;
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }
        Instant now = clock.instant();
        Window window = windows.compute(request.getRequestURI() + "|" + request.getRemoteAddr(),
                (key, current) -> current == null || current.isOverAt(now) ? new Window(now, 1) : current.next());
        if (window.count() > limit) {
            reject(response, Duration.between(now, window.end()));
            return;
        }
        forgetOldWindows(now);
        chain.doFilter(request, response);
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
