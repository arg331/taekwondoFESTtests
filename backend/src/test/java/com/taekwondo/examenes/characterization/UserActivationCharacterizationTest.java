package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: desactivar y reactivar usuarios (RF-05)")
class UserActivationCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Desactivar: cierra sus sesiones y no puede entrar; al reactivarlo entra, pero las sesiones viejas no vuelven")
    void deactivate_thenReactivate() {
        Session student = newStudent();

        HttpResult off = setActive(student.id(), adminToken(), false).expectStatus(200);

        assertThat((Boolean) off.read("$.active")).isFalse();
        get("/api/auth/me", student.token()).expectStatus(401);
        post("/api/auth/login", null, obj("usernameOrEmail", student.username(), "plainPassword", STUDENT_PASSWORD))
                .expectStatus(401);

        HttpResult on = setActive(student.id(), adminToken(), true).expectStatus(200);

        assertThat((Boolean) on.read("$.active")).isTrue();
        get("/api/auth/me", student.token()).expectStatus(401);
        assertThat(login(student.username(), STUDENT_PASSWORD)).isNotBlank();
    }

    @Test
    @DisplayName("Desactivar: repetirlo no cambia nada; el listado sigue mostrando a los desactivados")
    void deactivate_idempotentAndListed() {
        Session student = newStudent();

        setActive(student.id(), adminToken(), false).expectStatus(200);
        setActive(student.id(), adminToken(), false).expectStatus(200);

        String path = "$[?(@.id == " + student.id() + ")].active";
        assertThat(get("/api/users", adminToken()).expectStatus(200).<List<Boolean>>read(path)).containsExactly(false);
    }

    @Test
    @DisplayName("Desactivar: solo el administrador, nunca su propia cuenta; id inexistente 404; sin valor 400")
    void deactivate_rules() {
        Session student = newStudent();
        long adminId = ((Number) get("/api/auth/me", adminToken()).read("$.id")).longValue();

        setActive(student.id(), newTeacher().token(), false).expectStatus(403);
        assertThat(setActive(adminId, adminToken(), false).expectStatus(409).message())
                .isEqualTo("No puedes desactivar tu propia cuenta");
        setActive(999_999, adminToken(), false).expectStatus(404);
        assertThat(patch("/api/users/" + student.id() + "/active", adminToken(), obj()).expectStatus(400).message())
                .startsWith("active");
    }

    private HttpResult setActive(long userId, String token, boolean active) {
        return patch("/api/users/" + userId + "/active", token, obj("active", active));
    }
}
