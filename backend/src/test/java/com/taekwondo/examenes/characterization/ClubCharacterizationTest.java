package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: clubes")
class ClubCharacterizationTest extends ApiCharacterizationTest {

    private static final String SOLO_GESTORES = "Solo el administrador y los profesores del club pueden gestionarlo";

    // ───── Crear ─────

    @Test
    @DisplayName("Crear: un profesor crea un club, pasa a ser miembro y el nombre se recorta (RF-38)")
    void create_byTeacher_joinsClub() {
        Session teacher = newTeacher();
        String name = unique("Club Badajoz");

        HttpResult club = post("/api/clubs", teacher.token(), obj("name", "  " + name + "  ")).expectStatus(201);

        assertThat(club.asMap()).containsOnlyKeys("id", "name");
        assertThat((String) club.read("$.name")).isEqualTo(name);
        assertThat((Integer) me(teacher).read("$.club.id")).isEqualTo((int) club.id());
        assertThat((String) me(teacher).read("$.club.name")).isEqualTo(name);
    }

    @Test
    @DisplayName("Crear: el nombre es único sin distinguir mayúsculas")
    void create_duplicateName_conflict() {
        String name = unique("Club Mérida");
        post("/api/clubs", newTeacher().token(), obj("name", name)).expectStatus(201);

        HttpResult duplicate = post("/api/clubs", newTeacher().token(), obj("name", name.toUpperCase()));

        assertThat(duplicate.expectStatus(409).message()).isEqualTo("Ya existe un club llamado «" + name.toUpperCase() + "»");
    }

    @Test
    @DisplayName("Crear: un profesor que ya está en un club no puede crear otro (RF-38)")
    void create_teacherAlreadyInClub_conflict() {
        Session teacher = newTeacher();
        String first = unique("Club A");
        post("/api/clubs", teacher.token(), obj("name", first)).expectStatus(201);

        HttpResult second = post("/api/clubs", teacher.token(), obj("name", unique("Club B")));

        assertThat(second.expectStatus(409).message()).isEqualTo("Ya perteneces al club «" + first + "»: no puedes crear otro");
    }

    @Test
    @DisplayName("Crear: el administrador crea clubes sin entrar en ellos; un alumno no puede (RF-41, RF-42)")
    void create_byAdminAndStudent() {
        post("/api/clubs", adminToken(), obj("name", unique("Club del admin"))).expectStatus(201);
        assertThat((Object) get("/api/auth/me", adminToken()).read("$.club")).isNull();

        post("/api/clubs", newStudent().token(), obj("name", unique("Club"))).expectStatus(403);
    }

    @Test
    @DisplayName("Crear: el nombre no puede estar vacío")
    void create_blankName_badRequest() {
        assertThat(post("/api/clubs", newTeacher().token(), obj("name", " ")).expectStatus(400).message()).startsWith("name");
    }

    // ───── Listar ─────

    @Test
    @DisplayName("Listar: cualquiera, incluso sin sesión, ve los clubes ordenados por nombre (para el registro, RF-41)")
    void list_public_sortedByName() {
        String suffix = unique("");
        post("/api/clubs", newTeacher().token(), obj("name", "zeta" + suffix)).expectStatus(201);
        post("/api/clubs", newTeacher().token(), obj("name", "Alfa" + suffix)).expectStatus(201);

        List<String> names = get("/api/clubs", null).expectStatus(200).asList().stream()
                .map(c -> (String) c.get("name")).filter(n -> n.endsWith(suffix)).toList();

        assertThat(names).containsExactly("Alfa" + suffix, "zeta" + suffix);
    }

    // ───── Gestionar ─────

    @Test
    @DisplayName("Gestionar: cualquier profesor del club lo renombra; uno de otro club no (RF-40)")
    void rename_onlyClubTeachersOrAdmin() {
        Session owner = newTeacher();
        long clubId = createClub(owner);
        Session colleague = newTeacher();
        put("/api/clubs/" + clubId + "/members/" + colleague.id(), owner.token(), null).expectStatus(200);
        String newName = unique("Renombrado");

        HttpResult byColleague = patch("/api/clubs/" + clubId, colleague.token(), obj("name", newName));
        HttpResult byOutsider = patch("/api/clubs/" + clubId, newTeacher().token(), obj("name", unique("Otro")));
        HttpResult byAdmin = patch("/api/clubs/" + clubId, adminToken(), obj("name", newName + " bis"));

        assertThat((String) byColleague.expectStatus(200).read("$.name")).isEqualTo(newName);
        assertThat(byOutsider.expectStatus(403).message()).isEqualTo(SOLO_GESTORES);
        assertThat((String) byAdmin.expectStatus(200).read("$.name")).isEqualTo(newName + " bis");
    }

    @Test
    @DisplayName("Miembros: el profesor añade y quita alumnos y profesores sin club, y ve la lista (RF-40)")
    void members_addListRemove() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);
        Session student = newStudent();

        HttpResult added = put("/api/clubs/" + clubId + "/members/" + student.id(), teacher.token(), null).expectStatus(200);
        assertThat(added.asMap()).containsOnlyKeys("id", "username", "displayName", "role");
        assertThat((Integer) me(student).read("$.club.id")).isEqualTo((int) clubId);
        assertThat(memberIds(clubId, teacher)).containsExactlyInAnyOrder(teacher.id(), student.id());

        delete("/api/clubs/" + clubId + "/members/" + student.id(), teacher.token()).expectStatus(204);

        assertThat((Object) me(student).read("$.club")).isNull();
        assertThat(memberIds(clubId, teacher)).containsExactly(teacher.id());
    }

    @Test
    @DisplayName("Miembros: un profesor no puede quitar a nadie de otro club a su club; el administrador sí lo cambia (RF-40, RF-42)")
    void members_userInAnotherClub() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);
        Session otherTeacher = newTeacher();
        long otherClub = createClub(otherTeacher);
        Session student = newStudent();
        put("/api/clubs/" + otherClub + "/members/" + student.id(), otherTeacher.token(), null).expectStatus(200);

        HttpResult byTeacher = put("/api/clubs/" + clubId + "/members/" + student.id(), teacher.token(), null);
        HttpResult byAdmin = put("/api/clubs/" + clubId + "/members/" + student.id(), adminToken(), null);

        assertThat(byTeacher.expectStatus(409).message()).startsWith("Ese usuario ya pertenece al club");
        byAdmin.expectStatus(200);
        assertThat((Integer) me(student).read("$.club.id")).isEqualTo((int) clubId);
    }

    @Test
    @DisplayName("Miembros: solo el administrador y los profesores del club ven y tocan la lista")
    void members_forbiddenForOthers() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);
        Session student = newStudent();
        put("/api/clubs/" + clubId + "/members/" + student.id(), teacher.token(), null).expectStatus(200);

        get("/api/clubs/" + clubId + "/members", student.token()).expectStatus(403);
        assertThat(get("/api/clubs/" + clubId + "/members", newTeacher().token()).expectStatus(403).message())
                .isEqualTo(SOLO_GESTORES);
        get("/api/clubs/" + clubId + "/members", adminToken()).expectStatus(200);
    }

    @Test
    @DisplayName("Miembros: quitar a quien no es del club es un conflicto")
    void members_removeNonMember_conflict() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);

        HttpResult result = delete("/api/clubs/" + clubId + "/members/" + newStudent().id(), teacher.token());

        assertThat(result.expectStatus(409).message()).isEqualTo("Ese usuario no es miembro del club");
    }

    // ───── Buscar a quién añadir ─────

    @Test
    @DisplayName("Candidatos: el profesor busca por nombre o usuario entre quienes no tienen club (RF-40)")
    void candidates_teacherSeesUsersWithoutClub() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);
        Session free = newStudent();
        Session member = newStudent();
        put("/api/clubs/" + clubId + "/members/" + member.id(), teacher.token(), null).expectStatus(200);
        Session inOtherClub = newStudent();
        put("/api/users/me/club", inOtherClub.token(), obj("clubId", createClub(newTeacher()))).expectStatus(200);

        List<Map<String, Object>> found = search(clubId, free.username(), teacher);

        assertThat(found).singleElement().satisfies(c -> {
            assertThat(c).containsOnlyKeys("id", "username", "displayName", "role", "clubName");
            assertThat(((Number) c.get("id")).longValue()).isEqualTo(free.id());
            assertThat(c.get("clubName")).isNull();
        });
        assertThat(search(clubId, member.username(), teacher)).isEmpty();
        assertThat(search(clubId, inOtherClub.username(), teacher)).isEmpty();
    }

    @Test
    @DisplayName("Candidatos: busca también por el nombre visible y sin distinguir mayúsculas")
    void candidates_byDisplayName() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);
        Session student = newStudent();

        List<Map<String, Object>> found = get("/api/clubs/" + clubId + "/candidates?q=" + student.username().toUpperCase(),
                teacher.token()).expectStatus(200).asList();

        assertThat(ids(found)).containsExactly(student.id());
    }

    @Test
    @DisplayName("Candidatos: el administrador ve también a los de otros clubes, con el nombre de su club (RF-42)")
    void candidates_adminSeesOtherClubs() {
        long clubId = createClub(newTeacher());
        Session otherTeacher = newTeacher();
        long otherClub = createClub(otherTeacher);
        Session student = newStudent();
        put("/api/clubs/" + otherClub + "/members/" + student.id(), otherTeacher.token(), null).expectStatus(200);

        List<Map<String, Object>> found = get("/api/clubs/" + clubId + "/candidates?q=" + student.username(), adminToken())
                .expectStatus(200).asList();

        assertThat(ids(found)).containsExactly(student.id());
        assertThat((String) found.get(0).get("clubName")).isEqualTo(get("/api/clubs", null).asList().stream()
                .filter(c -> ((Number) c.get("id")).longValue() == otherClub).findFirst().orElseThrow().get("name"));
    }

    @Test
    @DisplayName("Candidatos: solo quien gestiona el club puede buscar")
    void candidates_forbiddenForOthers() {
        long clubId = createClub(newTeacher());

        get("/api/clubs/" + clubId + "/candidates?q=a", newTeacher().token()).expectStatus(403);
    }

        // ───── Elegir el propio club ─────

    @Test
    @DisplayName("Mi club: cada usuario elige su club, lo cambia o lo deja (RF-41)")
    void myClub_joinSwitchLeave() {
        long first = createClub(newTeacher());
        long second = createClub(newTeacher());
        Session student = newStudent();

        HttpResult joined = put("/api/users/me/club", student.token(), obj("clubId", first)).expectStatus(200);
        assertThat((Integer) joined.read("$.club.id")).isEqualTo((int) first);

        put("/api/users/me/club", student.token(), obj("clubId", second)).expectStatus(200);
        assertThat((Integer) me(student).read("$.club.id")).isEqualTo((int) second);

        put("/api/users/me/club", student.token(), obj("clubId", null)).expectStatus(200);
        assertThat((Object) me(student).read("$.club")).isNull();
    }

    @Test
    @DisplayName("Mi club: un club que no existe da 404")
    void myClub_unknown_notFound() {
        put("/api/users/me/club", newStudent().token(), obj("clubId", 999_999)).expectStatus(404);
    }

    // ───── Borrar ─────

    @Test
    @DisplayName("Borrar: solo el administrador; los miembros se quedan sin club (RF-42)")
    void delete_onlyAdmin_membersLeft() {
        Session teacher = newTeacher();
        long clubId = createClub(teacher);

        delete("/api/clubs/" + clubId, teacher.token()).expectStatus(403);
        delete("/api/clubs/" + clubId, adminToken()).expectStatus(204);

        assertThat((Object) me(teacher).read("$.club")).isNull();
        assertThat(get("/api/clubs", null).asList()).noneMatch(c -> ((Number) c.get("id")).longValue() == clubId);
        get("/api/clubs/" + clubId + "/members", adminToken()).expectStatus(404);
    }

    // ───── Ayudas ─────

    private long createClub(Session teacher) {
        return post("/api/clubs", teacher.token(), obj("name", unique("Club"))).expectStatus(201).id();
    }

    private List<Map<String, Object>> search(long clubId, String q, Session session) {
        return get("/api/clubs/" + clubId + "/candidates?q=" + q, session.token()).expectStatus(200).asList();
    }

    private static List<Long> ids(List<Map<String, Object>> users) {
        return users.stream().map(u -> ((Number) u.get("id")).longValue()).toList();
    }

    private HttpResult me(Session session) {
        return get("/api/auth/me", session.token()).expectStatus(200);
    }

    private List<Long> memberIds(long clubId, Session session) {
        return get("/api/clubs/" + clubId + "/members", session.token()).expectStatus(200).asList().stream()
                .map((Map<String, Object> m) -> ((Number) m.get("id")).longValue()).toList();
    }
}
