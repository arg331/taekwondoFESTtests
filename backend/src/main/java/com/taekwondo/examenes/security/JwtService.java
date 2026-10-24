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
    private static final String VERSION_CLAIM = "ver";
    private static final String EXAM_CLAIM = "exam";

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

    /** La versión de credenciales del usuario va en el token: al cambiar la contraseña, las anteriores dejan de valer. */
    public String generateAccessToken(Long userId, int credentialsVersion) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, ACCESS_TYPE)
                .claim(VERSION_CLAIM, credentialsVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(signingKey)
                .compact();
    }

    /** Los tokens emitidos antes de existir la versión cuentan como versión 0. */
    public Optional<AccessToken> parseAccessToken(String token) {
        return parse(token, ACCESS_TYPE).map(claims -> {
            Integer version = claims.get(VERSION_CLAIM, Integer.class);
            return new AccessToken(Long.parseLong(claims.getSubject()), version == null ? 0 : version);
        });
    }

    public record AccessToken(Long userId, int credentialsVersion) {}

    /** Token del intento: identifica el intento registrado (DE-04) y caduca con su plazo. */
    public String generateAttemptToken(Long attemptId, Long examId, Instant startedAt, Duration validity) {
        return Jwts.builder()
                .subject(String.valueOf(attemptId))
                .claim(TYPE_CLAIM, ATTEMPT_TYPE)
                .claim(EXAM_CLAIM, examId)
                .issuedAt(Date.from(startedAt))
                .expiration(Date.from(startedAt.plus(validity)))
                .signWith(signingKey)
                .compact();
    }

    public Optional<Attempt> parseAttemptToken(String token) {
        return parse(token, ATTEMPT_TYPE)
                .filter(claims -> claims.get(EXAM_CLAIM) instanceof Number)
                .map(claims -> new Attempt(Long.parseLong(claims.getSubject()),
                        ((Number) claims.get(EXAM_CLAIM)).longValue()));
    }

    public record Attempt(Long attemptId, Long examId) {}

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
