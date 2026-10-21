package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import java.time.Duration;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-07: límite de intentos por dirección en el inicio de sesión, el registro y la entrega de exámenes.
 * Aquí con límites pequeños para probarlos; el resto de tests los tienen desactivados.
 */
@SpringBootTest(properties = {
        "app.attempts.auto-submit.enabled=false",
        "app.rate-limit.enabled=true",
        "app.rate-limit.login-per-minute=3",
        "app.rate-limit.register-per-minute=2",
        "app.rate-limit.submit-per-minute=2"})
@DisplayName("Caracterización: límite de intentos")
class RateLimitCharacterizationTest extends ApiCharacterizationTest {

    private static final String TOO_MANY = "Demasiados intentos. Espera un minuto y vuelve a intentarlo.";

    /** Cada test empieza en un minuto nuevo, con los contadores a cero. */
    @BeforeEach
    void newWindow() {
        clock.advance(Duration.ofMinutes(2));
    }

    @Test
    @DisplayName("Inicio de sesión: pasado el límite responde 429 con el formato de error y Retry-After; al minuto se puede otra vez")
    void login_limited() throws Exception {
        for (int i = 0; i < 3; i++) {
            badLogin().expectStatus(401);
        }

        MockHttpServletResponse blocked = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"nadie\",\"plainPassword\":\"mala123\"}"))
                .andReturn().getResponse();
        HttpResult result = new HttpResult(blocked.getStatus(), blocked.getContentAsString());

        assertThat(result.expectStatus(429).asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
        assertThat(result.message()).isEqualTo(TOO_MANY);
        assertThat(Integer.parseInt(blocked.getHeader("Retry-After"))).isBetween(1, 60);
        // Los demás límites van aparte
        register().expectStatus(201);

        clock.advance(Duration.ofSeconds(61));
        badLogin().expectStatus(401);
    }

    @Test
    @DisplayName("Registro: pasado el límite, 429")
    void register_limited() {
        register().expectStatus(201);
        register().expectStatus(201);

        assertThat(register().expectStatus(429).message()).isEqualTo(TOO_MANY);
    }

    @Test
    @DisplayName("Entrega de exámenes: pasado el límite, 429, aunque las anteriores fueran inválidas")
    void submit_limited() {
        String body = "{\"examCode\":\"EXM-NOEXISTE\",\"attemptToken\":\"x\",\"answers\":[]}";
        post("/api/results", null, body).expectStatus(404);
        post("/api/results", null, body).expectStatus(404);

        assertThat(post("/api/results", null, body).expectStatus(429).message()).isEqualTo(TOO_MANY);
    }

    private HttpResult badLogin() {
        return post("/api/auth/login", null, obj("usernameOrEmail", "nadie", "plainPassword", "mala123"));
    }

    private HttpResult register() {
        String username = unique("limite");
        return post("/api/auth/register", null, obj("username", username, "plainPassword", "secreto1",
                "displayName", "Alumno " + username));
    }
}
