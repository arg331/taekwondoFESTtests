package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: contraseñas (cambiar la propia, restablecer, temporal)")
class PasswordCharacterizationTest extends ApiCharacterizationTest {

    private static final String NEW_PASSWORD = "nueva-clave-1";
    private static final String TEMPORARY = "temporal-99";
    private static final String MUST_CHANGE = "Tienes que cambiar la contraseña temporal antes de seguir";

    // ───── Cambiar la propia (RF-06) ─────

    @Test
    @DisplayName("Cambiar la propia: con la actual correcta devuelve una sesión nueva y la contraseña cambia")
    void changeOwn_valid_newSessionAndPassword() {
        Session student = newStudent();

        HttpResult result = changeOwn(student.token(), STUDENT_PASSWORD, NEW_PASSWORD).expectStatus(200);

        assertThat(result.asMap()).containsOnlyKeys("token", "user");
        assertThat((Boolean) result.read("$.user.mustChangePassword")).isFalse();
        get("/api/auth/me", (String) result.read("$.token")).expectStatus(200);
        assertThat(login(student.username(), NEW_PASSWORD)).isNotBlank();
        post("/api/auth/login", null, obj("usernameOrEmail", student.username(), "plainPassword", STUDENT_PASSWORD))
                .expectStatus(401);
    }

    @Test
    @DisplayName("Cambiar la propia: cierra las demás sesiones abiertas")
    void changeOwn_closesOtherSessions() {
        Session student = newStudent();
        String otherDevice = login(student.username(), STUDENT_PASSWORD);

        changeOwn(student.token(), STUDENT_PASSWORD, NEW_PASSWORD).expectStatus(200);

        get("/api/auth/me", otherDevice).expectStatus(401);
        get("/api/auth/me", student.token()).expectStatus(401);
    }

    @Test
    @DisplayName("Cambiar la propia: la actual mal, la nueva igual o la nueva corta se rechazan")
    void changeOwn_invalid() {
        Session student = newStudent();

        assertThat(changeOwn(student.token(), "otra-cosa", NEW_PASSWORD).expectStatus(409).message())
                .isEqualTo("La contraseña actual no es correcta");
        assertThat(changeOwn(student.token(), STUDENT_PASSWORD, STUDENT_PASSWORD).expectStatus(409).message())
                .isEqualTo("La nueva contraseña tiene que ser distinta de la actual");
        assertThat(changeOwn(student.token(), STUDENT_PASSWORD, "12345").expectStatus(400).message())
                .startsWith("Contraseña nueva: ");
        changeOwn(null, STUDENT_PASSWORD, NEW_PASSWORD).expectStatus(401);
    }

    // ───── Restablecer (RF-35) y contraseña temporal (RF-33) ─────

    @Test
    @DisplayName("Restablecer: el administrador pone una temporal, la cuenta queda marcada y sus sesiones se cierran")
    void reset_byAdmin_marksAndClosesSessions() {
        Session student = newStudent();

        HttpResult result = reset(student.id(), adminToken(), TEMPORARY).expectStatus(200);

        assertThat((Boolean) result.read("$.mustChangePassword")).isTrue();
        get("/api/auth/me", student.token()).expectStatus(401);
        HttpResult login = post("/api/auth/login", null,
                obj("usernameOrEmail", student.username(), "plainPassword", TEMPORARY)).expectStatus(200);
        assertThat((Boolean) login.read("$.user.mustChangePassword")).isTrue();
    }

    @Test
    @DisplayName("Temporal: hasta cambiarla solo se puede ver quién eres y cambiar la contraseña (RF-33)")
    void temporary_blocksEverythingButChange() {
        Session teacher = newTeacher();
        reset(teacher.id(), adminToken(), TEMPORARY).expectStatus(200);
        String token = login(teacher.username(), TEMPORARY);

        assertThat(get("/api/exams/mine", token).expectStatus(403).message()).isEqualTo(MUST_CHANGE);
        assertThat(get("/api/results/me", token).expectStatus(403).message()).isEqualTo(MUST_CHANGE);
        get("/api/auth/me", token).expectStatus(200);

        String fresh = changeOwn(token, TEMPORARY, NEW_PASSWORD).expectStatus(200).read("$.token");

        assertThat((Boolean) get("/api/auth/me", fresh).read("$.mustChangePassword")).isFalse();
        get("/api/exams/mine", fresh).expectStatus(200);
    }

    @Test
    @DisplayName("Restablecer: sirve aunque el usuario no tenga email (RF-35)")
    void reset_userWithoutEmail() {
        String username = unique("SinEmail");
        long id = post("/api/auth/register", null, obj(
                "username", username, "plainPassword", STUDENT_PASSWORD, "displayName", "Sin email")).expectStatus(201).id();

        reset(id, adminToken(), TEMPORARY).expectStatus(200);

        assertThat(login(username, TEMPORARY)).isNotBlank();
    }

    @Test
    @DisplayName("Restablecer: solo el administrador, no sobre sí mismo, con una temporal de al menos 6 caracteres")
    void reset_rules() {
        Session student = newStudent();
        long adminId = ((Number) get("/api/auth/me", adminToken()).read("$.id")).longValue();

        reset(student.id(), newTeacher().token(), TEMPORARY).expectStatus(403);
        assertThat(reset(adminId, adminToken(), TEMPORARY).expectStatus(409).message())
                .isEqualTo("Para tu propia contraseña usa «Cambiar contraseña»");
        assertThat(reset(student.id(), adminToken(), "12345").expectStatus(400).message()).startsWith("Contraseña temporal");
        reset(999_999, adminToken(), TEMPORARY).expectStatus(404);
    }

    // ───── Ayudas ─────

    private HttpResult changeOwn(String token, String current, String next) {
        return put("/api/auth/me/password", token, obj("currentPassword", current, "newPassword", next));
    }

    private HttpResult reset(long userId, String token, String temporary) {
        return put("/api/users/" + userId + "/password", token, obj("temporaryPassword", temporary));
    }
}
