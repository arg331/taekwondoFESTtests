package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: registro, login y gestión de usuarios")
class AuthAndUsersCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> USER_FIELDS =
            List.of("id", "username", "email", "displayName", "role", "active", "createdAt");

    // ───── Registro ─────

    @Test
    @DisplayName("Registro: crea un STUDENT activo, pasa el email a minúsculas y no devuelve la contraseña")
    void register_validData_createsActiveStudent() {
        String username = unique("Nuevo");

        HttpResult result = post("/api/auth/register", null, obj(
                "username", username,
                "email", username.toUpperCase() + "@Test.COM",
                "plainPassword", "secreto1",
                "displayName", "  Nombre Visible  ")).expectStatus(201);

        assertThat(result.asMap()).containsOnlyKeys(USER_FIELDS);
        assertThat((String) result.read("$.username")).isEqualTo(username);
        assertThat((String) result.read("$.email")).isEqualTo(username.toLowerCase() + "@test.com");
        assertThat((String) result.read("$.displayName")).isEqualTo("Nombre Visible");
        assertThat((String) result.read("$.role")).isEqualTo("STUDENT");
        assertThat((Boolean) result.read("$.active")).isTrue();
        assertThat((String) result.read("$.createdAt")).isNotBlank();
    }

    @Test
    @DisplayName("Registro: email con espacios alrededor se rechaza (400); sin Accept-Language el mensaje estándar sale en inglés")
    void register_emailWithSurroundingSpaces_rejectedWithEnglishMessage() {
        String username = unique("Espacios");

        HttpResult result = post("/api/auth/register", null, obj(
                "username", username,
                "email", "  " + username + "@test.com ",
                "plainPassword", "secreto1",
                "displayName", "Nombre"));

        assertThat(result.expectStatus(400).message()).isEqualTo("email: must be a well-formed email address");
    }

    @Test
    @DisplayName("Registro: usuario o email repetido (email sin distinguir mayúsculas) devuelve 409")
    void register_duplicateUsernameOrEmail_conflict() {
        Session existing = newStudent();

        HttpResult sameUsername = post("/api/auth/register", null, obj(
                "username", existing.username(), "email", unique("x") + "@test.com",
                "plainPassword", "secreto1", "displayName", "X"));
        HttpResult sameEmail = post("/api/auth/register", null, obj(
                "username", unique("otro"), "email", existing.username().toUpperCase() + "@TEST.com",
                "plainPassword", "secreto1", "displayName", "X"));

        assertThat(sameUsername.expectStatus(409).message())
                .isEqualTo("Ya existe un usuario con username '" + existing.username() + "'");
        assertThat(sameEmail.expectStatus(409).message())
                .isEqualTo("Ya existe un usuario con email '" + existing.username().toLowerCase() + "@test.com'");
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
            "username, ab",
            "username, con espacios",
            "email, no-es-un-email",
            "plainPassword, 12345",
            "displayName, ' '"
    })
    @DisplayName("Registro: datos inválidos devuelven 400 con el campo en el mensaje")
    void register_invalidField_badRequest(String field, String value) {
        Map<String, Object> body = obj(
                "username", unique("valido"), "email", unique("valido") + "@test.com",
                "plainPassword", "secreto1", "displayName", "Válido");
        body.put(field, value);

        HttpResult result = post("/api/auth/register", null, body).expectStatus(400);

        assertThat(result.message()).startsWith(field + ": ");
        assertThat(result.asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
    }

    // ───── Login ─────

    @Test
    @DisplayName("Login: por usuario o por email (sin distinguir mayúsculas) devuelve token y usuario")
    void login_byUsernameOrEmail_returnsTokenAndUser() {
        Session student = newStudent();

        HttpResult byUsername = post("/api/auth/login", null,
                obj("usernameOrEmail", student.username(), "plainPassword", STUDENT_PASSWORD)).expectStatus(200);
        HttpResult byEmail = post("/api/auth/login", null,
                obj("usernameOrEmail", student.username().toUpperCase() + "@TEST.COM",
                        "plainPassword", STUDENT_PASSWORD)).expectStatus(200);

        assertThat(byUsername.asMap()).containsOnlyKeys("token", "user");
        assertThat((Integer) byUsername.read("$.user.id")).isEqualTo((int) student.id());
        assertThat((String) byEmail.read("$.token")).isNotBlank();
    }

    @Test
    @DisplayName("Login: contraseña incorrecta o usuario inexistente devuelven el mismo 401")
    void login_wrongCredentials_unauthorizedWithSameMessage() {
        Session student = newStudent();

        HttpResult wrongPassword = post("/api/auth/login", null,
                obj("usernameOrEmail", student.username(), "plainPassword", "incorrecta"));
        HttpResult unknownUser = post("/api/auth/login", null,
                obj("usernameOrEmail", unique("nadie"), "plainPassword", "incorrecta"));

        assertThat(wrongPassword.expectStatus(401).message()).isEqualTo("Credenciales inválidas");
        assertThat(unknownUser.expectStatus(401).message()).isEqualTo("Credenciales inválidas");
    }

    @Test
    @DisplayName("Me: devuelve el usuario de la sesión")
    void me_withSession_returnsCurrentUser() {
        Session student = newStudent();

        HttpResult result = get("/api/auth/me", student.token()).expectStatus(200);

        assertThat(result.asMap()).containsOnlyKeys(USER_FIELDS);
        assertThat((String) result.read("$.username")).isEqualTo(student.username());
    }

    @Test
    @DisplayName("Seeder: existe el admin inicial con rol ADMIN")
    void seeder_createsInitialAdmin() {
        HttpResult me = get("/api/auth/me", adminToken()).expectStatus(200);

        assertThat((String) me.read("$.username")).isEqualTo(ADMIN_USERNAME);
        assertThat((String) me.read("$.role")).isEqualTo("ADMIN");
        assertThat((String) me.read("$.email")).isEqualTo("admin@taekwondo.local");
    }

    // ───── Gestión de usuarios ─────

    @Test
    @DisplayName("Usuarios: el listado incluye a todos, ordenados por fecha de alta")
    void listUsers_includesEveryone_orderedByCreation() {
        Session first = newStudent();
        Session second = newStudent();

        List<Map<String, Object>> users = get("/api/users", adminToken()).expectStatus(200).asList();

        List<Object> usernames = users.stream().map(u -> u.get("username")).toList();
        assertThat(usernames).contains(ADMIN_USERNAME);
        assertThat(usernames.indexOf(first.username())).isLessThan(usernames.indexOf(second.username()));
        assertThat(users.get(0)).containsOnlyKeys(USER_FIELDS);
    }

    @ParameterizedTest(name = "STUDENT → {0}")
    @CsvSource({"TEACHER", "ADMIN"})
    @DisplayName("Roles: el admin puede dar a un alumno cualquier rol")
    void changeRole_byAdmin_setsAnyRole(String role) {
        Session student = newStudent();

        HttpResult result = patch("/api/users/" + student.id() + "/role", adminToken(), obj("role", role));

        assertThat((String) result.expectStatus(200).read("$.role")).isEqualTo(role);
        assertThat(result.asMap()).containsOnlyKeys(USER_FIELDS);
    }

    @Test
    @DisplayName("Roles: dar el rol que ya tiene da 409")
    void changeRole_sameRole_conflict() {
        Session teacher = newTeacher();

        HttpResult result = patch("/api/users/" + teacher.id() + "/role", adminToken(), obj("role", "TEACHER"));

        assertThat(result.expectStatus(409).message()).isEqualTo("El usuario ya tiene el rol TEACHER");
    }

    @Test
    @DisplayName("Roles: un profesor no puede cambiar roles (RF-36)")
    void changeRole_byTeacher_forbidden() {
        Session teacher = newTeacher();
        Session student = newStudent();

        HttpResult result = patch("/api/users/" + student.id() + "/role", teacher.token(), obj("role", "TEACHER"));

        assertThat(result.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Roles: nadie puede cambiarse su propio rol")
    void changeRole_self_conflict() {
        Session otherAdmin = newUserWithRole("ADMIN");

        HttpResult result = patch("/api/users/" + otherAdmin.id() + "/role", otherAdmin.token(),
                obj("role", "STUDENT"));

        assertThat(result.expectStatus(409).message()).isEqualTo("No puedes cambiar tu propio rol");
    }

    @Test
    @DisplayName("Roles: sin rol o con un rol inexistente da 400")
    void changeRole_invalidRole_badRequest() {
        Session student = newStudent();
        String url = "/api/users/" + student.id() + "/role";

        assertThat(patch(url, adminToken(), obj()).expectStatus(400).message()).isEqualTo("role: must not be null");
        assertThat(patch(url, adminToken(), obj("role", "SUPERADMIN")).expectStatus(400).message())
                .isEqualTo("JSON malformado o petición inválida");
    }

    @Test
    @DisplayName("Roles: id inexistente devuelve 404")
    void changeRole_unknownUser_notFound() {
        HttpResult result = patch("/api/users/999999/role", adminToken(), obj("role", "TEACHER"));

        assertThat(result.expectStatus(404).message()).isEqualTo("Usuario no encontrado con id 999999");
    }

    @Test
    @DisplayName("Roles: un profesor no gestiona usuarios")
    void listUsers_byTeacher_forbidden() {
        assertThat(get("/api/users", newTeacher().token()).status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Roles: al pasar un profesor a alumno, su token pierde el acceso de profesor al instante")
    void teacherTurnedStudent_losesAccessImmediately() {
        Session teacher = newTeacher();
        assertThat(get("/api/questions", teacher.token()).status()).isEqualTo(200);

        patch("/api/users/" + teacher.id() + "/role", adminToken(), obj("role", "STUDENT")).expectStatus(200);

        assertThat(get("/api/questions", teacher.token()).status()).isEqualTo(403);
    }
}
