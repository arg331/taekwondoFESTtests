package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static com.taekwondo.examenes.characterization.support.Json.write;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-07: límite de intentos. En el inicio de sesión solo cuentan los fallos: por dirección y minuto, y por
 * cuenta (5 seguidos → 30 s de espera). El registro y la entrega de exámenes, por dirección y minuto.
 * Aquí con límites pequeños para probarlos; el resto de tests los tienen desactivados.
 */
@SpringBootTest(properties = {
        "app.attempts.auto-submit.enabled=false",
        "app.rate-limit.enabled=true",
        "app.rate-limit.login-failures-per-minute=12",
        "app.rate-limit.register-per-minute=2",
        "app.rate-limit.submit-per-minute=2"})
@DisplayName("Caracterización: límite de intentos")
class RateLimitCharacterizationTest extends ApiCharacterizationTest {

    private static final String TOO_MANY = "Demasiados intentos. Espera un minuto y vuelve a intentarlo.";
    private static final String ACCOUNT_LOCKED =
            "Demasiados intentos fallidos con esta cuenta. Espera 30 segundos y vuelve a intentarlo.";

    /** Cada test empieza en un minuto nuevo, con los contadores por dirección a cero. */
    @BeforeEach
    void newWindow() {
        clock.advance(Duration.ofMinutes(2));
    }

    @Test
    @DisplayName("Inicio de sesión: los correctos no cuentan; una clase entera entra desde la misma dirección")
    void login_successesDoNotCount() {
        Session student = newStudent();

        for (int i = 0; i < 40; i++) {
            login(student.username(), STUDENT_PASSWORD);
        }
    }

    @Test
    @DisplayName("Inicio de sesión: al 5.º fallo seguido la cuenta espera 30 s, aunque luego acierte; después entra")
    void login_accountWaitsAfterFiveFailures() throws Exception {
        Session student = newStudent();
        for (int i = 0; i < 5; i++) {
            badLogin(student.username()).expectStatus(401);
        }

        MockHttpServletResponse locked = rawLogin(student.username(), STUDENT_PASSWORD);
        HttpResult result = new HttpResult(locked.getStatus(), locked.getContentAsString(StandardCharsets.UTF_8));

        assertThat(result.expectStatus(429).asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
        assertThat(result.message()).isEqualTo(ACCOUNT_LOCKED);
        assertThat(Integer.parseInt(locked.getHeader("Retry-After"))).isBetween(1, 30);

        clock.advance(Duration.ofSeconds(31));
        login(student.username(), STUDENT_PASSWORD);
    }

    @Test
    @DisplayName("Inicio de sesión: la espera vale también entrando por el email de la cuenta")
    void login_accountLockCoversEmail() {
        Session student = newStudent();
        for (int i = 0; i < 5; i++) {
            badLogin(student.username()).expectStatus(401);
        }

        assertThat(post("/api/auth/login", null, obj("usernameOrEmail", student.username() + "@test.com",
                "plainPassword", STUDENT_PASSWORD)).expectStatus(429).message()).isEqualTo(ACCOUNT_LOCKED);
    }

    @Test
    @DisplayName("Inicio de sesión: un acierto pone a cero los fallos de la cuenta")
    void login_successResetsAccountFailures() {
        Session student = newStudent();
        for (int i = 0; i < 4; i++) {
            badLogin(student.username()).expectStatus(401);
        }
        login(student.username(), STUDENT_PASSWORD);

        for (int i = 0; i < 4; i++) {
            badLogin(student.username()).expectStatus(401);
        }
        login(student.username(), STUDENT_PASSWORD);
    }

    @Test
    @DisplayName("Inicio de sesión: la espera de una cuenta no afecta a otra que entra desde la misma dirección")
    void login_accountLockIsPerAccount() {
        Session locked = newStudent();
        Session other = newStudent();
        for (int i = 0; i < 5; i++) {
            badLogin(locked.username()).expectStatus(401);
        }

        badLogin(locked.username()).expectStatus(429);
        login(other.username(), STUDENT_PASSWORD);
    }

    @Test
    @DisplayName("Inicio de sesión: pasados los fallos por dirección y minuto, 429 para cualquier cuenta; al minuto, otra vez")
    void login_failuresPerAddressLimited() throws Exception {
        Session student = newStudent();
        for (int i = 0; i < 12; i++) {
            badLogin(unique("nadie")).expectStatus(401);
        }

        MockHttpServletResponse blocked = rawLogin(student.username(), STUDENT_PASSWORD);
        HttpResult result = new HttpResult(blocked.getStatus(), blocked.getContentAsString(StandardCharsets.UTF_8));

        assertThat(result.expectStatus(429).message()).isEqualTo(TOO_MANY);
        assertThat(Integer.parseInt(blocked.getHeader("Retry-After"))).isBetween(1, 60);
        // Los demás límites van aparte
        register().expectStatus(201);

        clock.advance(Duration.ofSeconds(61));
        login(student.username(), STUDENT_PASSWORD);
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

    private HttpResult badLogin(String usernameOrEmail) {
        return post("/api/auth/login", null, obj("usernameOrEmail", usernameOrEmail, "plainPassword", "mala123"));
    }

    private MockHttpServletResponse rawLogin(String usernameOrEmail, String password) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(write(obj("usernameOrEmail", usernameOrEmail, "plainPassword", password))))
                .andReturn().getResponse();
    }

    private HttpResult register() {
        String username = unique("limite");
        return post("/api/auth/register", null, obj("username", username, "plainPassword", "secreto1",
                "displayName", "Alumno " + username));
    }
}
