package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: registro, login y gestión de usuarios")
class AuthAndUsersCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> USER_FIELDS =
            List.of("id", "username", "email", "displayName", "role", "club", "active", "mustChangePassword", "createdAt");

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sin el usuario «user» que Spring crea por defecto: solo se entra con JWT y no se escribe su contraseña en el log")
    void noDefaultInMemoryUser() {
        assertThat(context.getBeansOfType(UserDetailsService.class)).isEmpty();
    }

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
    @DisplayName("Registro: el email es opcional; sin él (ausente, null o vacío) el usuario queda sin email (RF-01)")
    void register_withoutEmail_created() {
        String first = unique("SinEmail");
        String second = unique("SinEmail");
        String third = unique("SinEmail");

        HttpResult absent = post("/api/auth/register", null, obj(
                "username", first, "plainPassword", "secreto1", "displayName", "Sin email")).expectStatus(201);
        HttpResult nullEmail = post("/api/auth/register", null, obj(
                "username", second, "email", null, "plainPassword", "secreto1", "displayName", "Sin email"))
                .expectStatus(201);
        HttpResult blank = post("/api/auth/register", null, obj(
                "username", third, "email", "", "plainPassword", "secreto1", "displayName", "Sin email"))
                .expectStatus(201);

        assertThat((Object) absent.read("$.email")).isNull();
        assertThat((Object) nullEmail.read("$.email")).isNull();
        assertThat((Object) blank.read("$.email")).isNull();
        assertThat(login(first, "secreto1")).isNotBlank();
    }

    @Test
    @DisplayName("Registro: se puede elegir un club existente; queda en él (RF-01, RF-41)")
    void register_withClub_joinsIt() {
        long clubId = post("/api/clubs", newTeacher().token(), obj("name", unique("Club"))).expectStatus(201).id();

        HttpResult result = post("/api/auth/register", null, obj(
                "username", unique("ConClub"), "plainPassword", "secreto1", "displayName", "Con club",
                "clubId", clubId)).expectStatus(201);

        assertThat((Integer) result.read("$.club.id")).isEqualTo((int) clubId);
        assertThat((String) result.read("$.role")).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Registro: un club que no existe da 404 y no crea el usuario")
    void register_unknownClub_notFound() {
        String username = unique("ClubFalso");

        post("/api/auth/register", null, obj(
                "username", username, "plainPassword", "secreto1", "displayName", "X", "clubId", 999_999))
                .expectStatus(404);

        post("/api/auth/login", null, obj("usernameOrEmail", username, "plainPassword", "secreto1")).expectStatus(401);
    }

    @Test
    @DisplayName("Registro: email con espacios alrededor se rechaza (400), con el mensaje en español (RNF-13)")
    void register_emailWithSurroundingSpaces_rejected() {
        String username = unique("Espacios");

        HttpResult result = post("/api/auth/register", null, obj(
                "username", username,
                "email", "  " + username + "@test.com ",
                "plainPassword", "secreto1",
                "displayName", "Nombre"));

        assertThat(result.expectStatus(400).message()).isEqualTo("Email: debe ser una dirección de correo electrónico con formato correcto");
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
            "username, ab, Usuario",
            "username, con espacios, Usuario",
            "email, no-es-un-email, Email",
            "plainPassword, 12345, Contraseña",
            "displayName, ' ', Nombre"
    })
    @DisplayName("Registro: datos inválidos devuelven 400 con el campo, como lo ve el usuario, en el mensaje (RNF-13)")
    void register_invalidField_badRequest(String field, String value, String label) {
        Map<String, Object> body = obj(
                "username", unique("valido"), "email", unique("valido") + "@test.com",
                "plainPassword", "secreto1", "displayName", "Válido");
        body.put(field, value);

        HttpResult result = post("/api/auth/register", null, body).expectStatus(400);

        assertThat(result.message()).startsWith(label + ": ");
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
    @DisplayName("Login: la sesión dura 8 horas; después hay que volver a entrar (DE-10)")
    void session_expiresAfterEightHours() {
        Session student = newStudent();

        clock.advance(Duration.ofHours(8).minusMinutes(1));
        get("/api/auth/me", student.token()).expectStatus(200);
        clock.advance(Duration.ofMinutes(2));
        get("/api/auth/me", student.token()).expectStatus(401);
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
    @DisplayName("Usuarios: por páginas de 20 (RNF-09), ordenados por fecha de alta, con el total")
    void listUsers_paged_orderedByCreation() {
        HttpResult firstPage = get("/api/users", adminToken()).expectStatus(200);

        assertThat(firstPage.asMap()).containsOnlyKeys("items", "page", "size", "totalItems", "totalPages");
        assertThat((Integer) firstPage.read("$.page")).isZero();
        assertThat((Integer) firstPage.read("$.size")).isEqualTo(20);
        assertThat((String) firstPage.read("$.items[0].username")).isEqualTo(ADMIN_USERNAME);
        assertThat((Map<String, Object>) firstPage.read("$.items[0]")).containsOnlyKeys(USER_FIELDS);

        String batch = unique("Lote");
        List<String> usernames = List.of(register(batch, 1), register(batch, 2), register(batch, 3));
        HttpResult page0 = get("/api/users?q=" + batch + "&size=2", adminToken()).expectStatus(200);
        HttpResult page1 = get("/api/users?q=" + batch + "&size=2&page=1", adminToken()).expectStatus(200);

        assertThat((List<String>) page0.read("$.items[*].username")).containsExactlyElementsOf(usernames.subList(0, 2));
        assertThat((List<String>) page1.read("$.items[*].username")).containsExactly(usernames.get(2));
        assertThat((Integer) page0.read("$.totalItems")).isEqualTo(3);
        assertThat((Integer) page0.read("$.totalPages")).isEqualTo(2);
        assertThat((Integer) page1.read("$.page")).isEqualTo(1);
    }

    @Test
    @DisplayName("Usuarios: se buscan por nombre, usuario o email, sin distinguir mayúsculas ni tildes (RF-37)")
    void listUsers_search() {
        String tag = unique("Busca");
        String username = register("Íñigo Pérez " + tag, 1);

        assertThat(searchUsernames("inigo perez " + tag)).containsExactly(username);
        assertThat(searchUsernames(username.toUpperCase())).containsExactly(username);
        assertThat(searchUsernames(username.toUpperCase() + "@TEST.COM")).containsExactly(username);
        assertThat(searchUsernames(unique("nadie"))).isEmpty();
    }

    @Test
    @DisplayName("Usuarios: como mucho 100 por página")
    void listUsers_sizeCapped() {
        assertThat((Integer) get("/api/users?size=500", adminToken()).expectStatus(200).read("$.size")).isEqualTo(100);
    }

    /** Alta con el nombre indicado; devuelve el usuario. */
    private String register(String displayName, int n) {
        String username = unique("u" + n);
        post("/api/auth/register", null, obj("username", username, "email", username + "@test.com",
                "plainPassword", STUDENT_PASSWORD, "displayName", displayName)).expectStatus(201);
        return username;
    }

    private List<String> searchUsernames(String q) {
        // MockMvc codifica la URL: el texto va tal cual
        return get("/api/users?q=" + q, adminToken()).expectStatus(200).read("$.items[*].username");
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

        assertThat(patch(url, adminToken(), obj()).expectStatus(400).message()).isEqualTo("Rol: no debe ser nulo");
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
