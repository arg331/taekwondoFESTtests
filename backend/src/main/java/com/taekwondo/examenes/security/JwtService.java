package com.taekwondo.examenes.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite y valida los dos tipos de token HS256 de la aplicación:
 * <ul>
 *   <li><b>access</b>: sesión de usuario; subject = userId.</li>
 *   <li><b>attempt</b>: intento de examen; subject = examId y issuedAt = hora de inicio.
 *       Permite controlar el tiempo límite sin guardar estado en el servidor.</li>
 * </ul>
 * El claim "typ" impide usar un tipo de token en lugar del otro.
 */
@Service
public class JwtService {

    private static final String TYPE_CLAIM = "typ";
    private static final String ACCESS_TYPE = "access";
    private static final String ATTEMPT_TYPE = "attempt";

    private final SecretKey signingKey;
    private final Duration accessTokenValidity;
    private final Clock clock;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-ms}") long expirationMs,
                      Clock clock) {
        // HS256 exige claves de al menos 256 bits; si es más corta, falla al arrancar.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofMillis(expirationMs);
        this.clock = clock;
    }

    public String generateAccessToken(Long userId) {
        return build(ACCESS_TYPE, String.valueOf(userId), clock.instant(), accessTokenValidity);
    }

    public Optional<Long> parseAccessToken(String token) {
        return parse(token, ACCESS_TYPE).map(claims -> Long.parseLong(claims.getSubject()));
    }

    public String generateAttemptToken(Long examId, Instant startedAt, Duration validity) {
        return build(ATTEMPT_TYPE, String.valueOf(examId), startedAt, validity);
    }

    public Optional<Attempt> parseAttemptToken(String token) {
        return parse(token, ATTEMPT_TYPE).map(claims -> new Attempt(
                Long.parseLong(claims.getSubject()), claims.getIssuedAt().toInstant()));
    }

    public record Attempt(Long examId, Instant startedAt) {}

    private String build(String type, String subject, Instant issuedAt, Duration validity) {
        return Jwts.builder()
                .subject(subject)
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(validity)))
                .signWith(signingKey)
                .compact();
    }

    private Optional<Claims> parse(String token, String expectedType) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            Long.parseLong(claims.getSubject());
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            // Firma incorrecta, caducado, malformado o subject no numérico.
            return Optional.empty();
        }
    }
}
