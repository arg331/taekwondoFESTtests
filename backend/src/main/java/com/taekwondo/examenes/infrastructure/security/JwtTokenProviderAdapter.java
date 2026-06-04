package com.taekwondo.examenes.infrastructure.security;

import com.taekwondo.examenes.domain.port.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Adaptador del puerto JwtTokenProvider usando la librería JJWT.
 *
 * Genera tokens HS256 con:
 *  - sub: userId del usuario
 *  - iat: fecha de emisión
 *  - exp: fecha de expiración (now + expirationMs)
 *
 * Decisión consciente: NO incluimos el role en el token. Eso significa
 * que el filtro JWT, tras extraer el userId, debe cargar el User desde
 * la BD para obtener el role. Coste: una consulta indexada por petición.
 * Beneficio: si el role cambia (o el usuario se desactiva), surte efecto
 * en la siguiente petición, no tras la expiración del token.
 */
@Component
public class JwtTokenProviderAdapter implements JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProviderAdapter(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs) {

        // JJWT exige al menos 256 bits (32 bytes) para HS256.
        // Si la clave es más corta, Keys.hmacShaKeyFor lanza WeakKeyException.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser null");
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<Long> extractUserId(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            return Optional.of(Long.parseLong(subject));

        } catch (JwtException | IllegalArgumentException ex) {
            // Token inválido, expirado, malformado o subject no numérico.
            // Tratamos todos los casos igual: el token no es válido.
            return Optional.empty();
        }
    }
}
