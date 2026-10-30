package com.taekwondo.examenes.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * RNF-02: con el perfil «prod» la aplicación no arranca con los secretos de desarrollo. Sin esto, cualquiera
 * que lea el repositorio podría firmar tokens de administrador o entrar con la contraseña inicial.
 */
@Component
@Profile("prod")
public class ProductionSecretsCheck {

    /** El de application.properties, solo para desarrollo. */
    static final String DEV_JWT_SECRET = "dev-only-secret-replace-in-production-min-32-chars-long";
    static final String DEV_ADMIN_PASSWORD = "admin123";
    /** HS256 necesita una clave de al menos 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final String jwtSecret;
    private final String adminPassword;

    public ProductionSecretsCheck(@Value("${app.security.jwt.secret}") String jwtSecret,
                                  @Value("${ADMIN_PASSWORD:" + DEV_ADMIN_PASSWORD + "}") String adminPassword) {
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
    }

    @PostConstruct
    void verify() {
        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("Define JWT_SECRET: en producción no vale la clave de desarrollo");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET tiene que tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException("Define ADMIN_PASSWORD: en producción no vale la contraseña de desarrollo");
        }
    }
}
