package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matriz de permisos: quién puede llamar a cada endpoint (anónimo, alumno, profesor, admin).
 * Se comprueba solo el acceso, no el resultado de negocio: los ids no existen a
 * propósito, así que quien tiene permiso recibe 4xx de negocio pero nunca 401 ni 403.
 */
@DisplayName("Caracterización: control de acceso por rol")
class AccessControlCharacterizationTest extends ApiCharacterizationTest {

    private static final String MISSING = "999999";

    /** Solo el administrador gestiona usuarios y roles (RF-36). */
    static Stream<Arguments> adminOnlyEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/users", null),
                Arguments.of(HttpMethod.PATCH, "/api/users/" + MISSING + "/role", "{}"));
    }

    /** Preparar exámenes: profesor y administrador. */
    static Stream<Arguments> teacherEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/tags", null),
                Arguments.of(HttpMethod.POST, "/api/tags", "{}"),
                Arguments.of(HttpMethod.GET, "/api/tags/" + MISSING, null),
                Arguments.of(HttpMethod.PATCH, "/api/tags/" + MISSING, "{}"),
                Arguments.of(HttpMethod.GET, "/api/questions", null),
                Arguments.of(HttpMethod.POST, "/api/questions", "{}"),
                Arguments.of(HttpMethod.GET, "/api/questions/search", null),
                Arguments.of(HttpMethod.GET, "/api/questions/" + MISSING, null),
                Arguments.of(HttpMethod.PUT, "/api/questions/" + MISSING, "{}"),
                Arguments.of(HttpMethod.DELETE, "/api/questions/" + MISSING, null),
                Arguments.of(HttpMethod.GET, "/api/exams/mine", null),
                Arguments.of(HttpMethod.GET, "/api/exams/" + MISSING, null),
                Arguments.of(HttpMethod.POST, "/api/exams/drafts", "{}"),
                Arguments.of(HttpMethod.POST, "/api/exams/drafts/pre-generated", "{}"),
                Arguments.of(HttpMethod.PATCH, "/api/exams/" + MISSING + "/title", "{}"),
                Arguments.of(HttpMethod.PATCH, "/api/exams/" + MISSING + "/config", "{}"),
                Arguments.of(HttpMethod.PUT, "/api/exams/" + MISSING + "/questions", "{}"),
                Arguments.of(HttpMethod.PATCH, "/api/exams/" + MISSING + "/visibility", "{}"),
                Arguments.of(HttpMethod.POST, "/api/exams/" + MISSING + "/publish", "{}"),
                Arguments.of(HttpMethod.POST, "/api/exams/" + MISSING + "/close", null),
                Arguments.of(HttpMethod.POST, "/api/exams/" + MISSING + "/reopen", "{}"),
                Arguments.of(HttpMethod.PATCH, "/api/exams/" + MISSING + "/expiration", "{}"),
                Arguments.of(HttpMethod.DELETE, "/api/exams/" + MISSING, null),
                Arguments.of(HttpMethod.POST, "/api/exams/" + MISSING + "/favorite", null),
                Arguments.of(HttpMethod.DELETE, "/api/exams/" + MISSING + "/favorite", null),
                Arguments.of(HttpMethod.GET, "/api/favorites", null),
                Arguments.of(HttpMethod.GET, "/api/results/" + MISSING, null),
                Arguments.of(HttpMethod.GET, "/api/results/exam/" + MISSING, null),
                Arguments.of(HttpMethod.GET, "/api/results/exam/" + MISSING + "/statistics", null));
    }

    static Stream<Arguments> anyUserEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/auth/me"),
                Arguments.of(HttpMethod.GET, "/api/results/me"),
                Arguments.of(HttpMethod.GET, "/api/exams/public"));
    }

    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/auth/register", "{}", 400),
                Arguments.of(HttpMethod.POST, "/api/auth/login", "{}", 400),
                Arguments.of(HttpMethod.GET, "/api/exams/by-code/EXM-NOEXISTE", null, 404),
                Arguments.of(HttpMethod.POST, "/api/exams/by-code/EXM-NOEXISTE/attempts", null, 404),
                Arguments.of(HttpMethod.POST, "/api/results", "{}", 400));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("adminOnlyEndpoints")
    @DisplayName("Endpoints de administrador: anónimo 401, alumno y profesor 403, admin pasa")
    void adminOnlyEndpoint_byRole_onlyAdminGetsThrough(HttpMethod method, String url, String body) {
        Session student = newStudent();
        Session teacher = newTeacher();

        assertThat(send(method, url, null, body).status()).isEqualTo(401);
        assertThat(send(method, url, student.token(), body).status()).isEqualTo(403);
        assertThat(send(method, url, teacher.token(), body).status()).isEqualTo(403);
        assertThat(send(method, url, adminToken(), body).status()).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("teacherEndpoints")
    @DisplayName("Endpoints de profesor: anónimo 401, alumno 403, profesor y admin pasan")
    void teacherEndpoint_byRole_teacherAndAdminGetThrough(HttpMethod method, String url, String body) {
        Session student = newStudent();
        Session teacher = newTeacher();

        assertThat(send(method, url, null, body).status()).isEqualTo(401);
        assertThat(send(method, url, student.token(), body).status()).isEqualTo(403);
        assertThat(send(method, url, teacher.token(), body).status()).isNotIn(401, 403);
        assertThat(send(method, url, adminToken(), body).status()).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("anyUserEndpoints")
    @DisplayName("Endpoints con sesión: anónimo 401, alumno, profesor y admin 200")
    void anyUserEndpoint_byRole_requiresSession(HttpMethod method, String url) {
        Session student = newStudent();
        Session teacher = newTeacher();

        assertThat(send(method, url, null, null).status()).isEqualTo(401);
        assertThat(send(method, url, student.token(), null).status()).isEqualTo(200);
        assertThat(send(method, url, teacher.token(), null).status()).isEqualTo(200);
        assertThat(send(method, url, adminToken(), null).status()).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("publicEndpoints")
    @DisplayName("Endpoints públicos: responden sin sesión")
    void publicEndpoint_withoutSession_reachesController(HttpMethod method, String url, String body, int expected) {
        assertThat(send(method, url, null, body).status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("401 y 403 de la capa de seguridad llegan sin cuerpo (RNF-05 parcial)")
    void securityRejections_haveEmptyBody() {
        Session student = newStudent();

        HttpResult unauthorized = get("/api/questions", null);
        HttpResult forbidden = get("/api/questions", student.token());

        assertThat(unauthorized.status()).isEqualTo(401);
        assertThat(unauthorized.body()).isEmpty();
        assertThat(forbidden.status()).isEqualTo(403);
        assertThat(forbidden.body()).isEmpty();
    }

    @Test
    @DisplayName("Token inválido o caducado se trata como anónimo")
    void invalidToken_isTreatedAsAnonymous() {
        assertThat(get("/api/auth/me", "esto-no-es-un-jwt").status()).isEqualTo(401);
        assertThat(get("/api/exams/by-code/EXM-NOEXISTE", "esto-no-es-un-jwt").status()).isEqualTo(404);
    }

    @Test
    @DisplayName("Ruta inexistente: anónimo 401; con sesión de profesor, 404 con mensaje")
    void unknownRoute_dependsOnSession() {
        assertThat(get("/api/no-existe", null).status()).isEqualTo(401);

        HttpResult asTeacher = get("/api/no-existe", adminToken()).expectStatus(404);

        assertThat(asTeacher.message()).isEqualTo("Recurso no encontrado: api/no-existe");
    }
}
