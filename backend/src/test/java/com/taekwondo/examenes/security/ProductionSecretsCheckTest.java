package com.taekwondo.examenes.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RNF-02: en producción no se arranca con los secretos de desarrollo. */
@DisplayName("Secretos en producción")
class ProductionSecretsCheckTest {

    private static final String GOOD_SECRET = "una-clave-larga-y-aleatoria-de-produccion-1234567890";
    private static final String GOOD_PASSWORD = "Contraseña-de-verdad";

    @Test
    @DisplayName("Con un secreto propio de 32 bytes o más y otra contraseña de admin, arranca")
    void validSecrets_start() {
        assertThatCode(() -> new ProductionSecretsCheck(GOOD_SECRET, GOOD_PASSWORD).verify()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Con el secreto de desarrollo, no arranca y dice qué definir")
    void defaultSecret_fails() {
        assertThatThrownBy(() -> new ProductionSecretsCheck(ProductionSecretsCheck.DEV_JWT_SECRET, GOOD_PASSWORD).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("Con un secreto de menos de 32 bytes, no arranca")
    void shortSecret_fails() {
        assertThatThrownBy(() -> new ProductionSecretsCheck("corta", GOOD_PASSWORD).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("Con la contraseña de admin de desarrollo, no arranca")
    void defaultAdminPassword_fails() {
        assertThatThrownBy(() -> new ProductionSecretsCheck(GOOD_SECRET, "admin123").verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
    }
}
