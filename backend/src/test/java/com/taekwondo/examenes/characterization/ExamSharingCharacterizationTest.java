package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-51: compartir un examen con otros profesores. El invitado acepta o rechaza; al aceptar, consulta
 * el mismo examen del dueño (no una copia) con todos sus resultados, igual que el dueño, pero no puede
 * cambiarlo. Resuelve DE-08: compartir ya no tiene que ver con la visibilidad.
 */
@DisplayName("Caracterización: compartir exámenes entre profesores")
class ExamSharingCharacterizationTest extends ApiCharacterizationTest {

    private static final String ALREADY = "Ya has compartido este examen con ese profesor";

    // ───── Invitar, aceptar y rechazar ─────

    @Test
    @DisplayName("La invitación le llega al otro profesor; hasta que la acepta no ve el examen")
    void invitation_pendingUntilAccepted() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        String title = get("/api/exams/" + exam.id(), owner.token()).expectStatus(200).read("$.title");

        HttpResult invited = invite(exam.id(), guest.id(), owner.token()).expectStatus(201);

        assertThat(invited.asMap()).containsOnlyKeys("teacherId", "username", "displayName", "status", "createdAt");
        assertThat((String) invited.read("$.status")).isEqualTo("PENDING");
        List<Map<String, Object>> invitations = invitations(guest.token());
        assertThat(invitations).hasSize(1);
        assertThat(invitations.get(0)).containsOnlyKeys("id", "examId", "examTitle", "invitedBy", "createdAt")
                .containsEntry("examTitle", title)
                .containsEntry("invitedBy", "Alumno " + owner.username());
        get("/api/exams/" + exam.id(), guest.token()).expectStatus(404);
        assertThat(mine(guest.token())).extracting(e -> ((Number) e.get("id")).longValue()).doesNotContain(exam.id());
    }

    @Test
    @DisplayName("Al aceptar, el examen sale en su lista con quién se lo compartió, y consulta sus resultados como el dueño")
    void accept_viewsExamAndResults() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        submit(exam.code(), startAttempt(exam.code(), null, unique("Ana")), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        invite(exam.id(), guest.id(), owner.token()).expectStatus(201);

        HttpResult accepted = post("/api/shares/invitations/" + invitationId(guest.token()) + "/accept", guest.token(), null)
                .expectStatus(200);

        assertThat((String) accepted.read("$.sharedBy")).isEqualTo("Alumno " + owner.username());
        assertThat(invitations(guest.token())).isEmpty();
        Map<String, Object> listed = mine(guest.token()).stream()
                .filter(e -> ((Number) e.get("id")).longValue() == exam.id()).findFirst().orElseThrow();
        assertThat(listed).containsEntry("sharedBy", "Alumno " + owner.username());
        assertThat((Object) get("/api/exams/" + exam.id(), owner.token()).read("$.sharedBy")).isNull();
        // Los mismos resultados que ve el dueño, con las respuestas correctas
        String path = "/api/results/exam/" + exam.id();
        assertThat(get(path, guest.token()).expectStatus(200).body()).isEqualTo(get(path, owner.token()).body());
        assertThat((List<?>) get(path, guest.token()).read("$[0].answers[0].correctAnswers")).isNotNull();
        get(path + "/statistics", guest.token()).expectStatus(200);
        get(path + "/questions", guest.token()).expectStatus(200);
        get(path + "/export", guest.token()).expectStatus(200);
        get("/api/exams/" + exam.id() + "/published-questions", guest.token()).expectStatus(200);
    }

    @Test
    @DisplayName("El invitado no puede cambiar el examen: todo lo que lo cambia responde como si no existiera (404)")
    void guest_cannotChangeExam() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        Session other = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        share(exam.id(), owner, guest);
        String base = "/api/exams/" + exam.id();

        patch(base + "/title", guest.token(), obj("newTitle", "Mío")).expectStatus(404);
        patch(base + "/attempts", guest.token(), obj("maxAttempts", 2)).expectStatus(404);
        patch(base + "/expiration", guest.token(), obj("newExpiresAt", now().plusDays(1).toString())).expectStatus(404);
        patch(base + "/visibility", guest.token(), obj("newVisibility", "PRIVATE")).expectStatus(404);
        post(base + "/close", guest.token(), null).expectStatus(404);
        delete(base, guest.token()).expectStatus(404);
        invite(exam.id(), other.id(), guest.token()).expectStatus(404);
        get(base + "/shares", guest.token()).expectStatus(404);
        assertThat((String) get(base, owner.token()).read("$.status")).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("Al rechazar, la invitación desaparece y el dueño puede volver a invitarle")
    void reject_canInviteAgain() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        invite(exam.id(), guest.id(), owner.token()).expectStatus(201);

        delete("/api/shares/invitations/" + invitationId(guest.token()), guest.token()).expectStatus(204);

        assertThat(invitations(guest.token())).isEmpty();
        assertThat(shares(exam.id(), owner.token())).isEmpty();
        invite(exam.id(), guest.id(), owner.token()).expectStatus(201);
    }

    @Test
    @DisplayName("Invitar: solo el dueño (404), a un profesor o administrador que exista, no a sí mismo ni dos veces (409)")
    void invite_rules() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        Session student = newStudent();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        invite(exam.id(), guest.id(), owner.token()).expectStatus(201);

        assertThat(invite(exam.id(), guest.id(), owner.token()).expectStatus(409).message()).isEqualTo(ALREADY);
        assertThat(invite(exam.id(), owner.id(), owner.token()).expectStatus(409).message())
                .isEqualTo("No puedes compartir un examen contigo mismo");
        assertThat(invite(exam.id(), student.id(), owner.token()).expectStatus(409).message())
                .isEqualTo("Solo se puede compartir con profesores");
        invite(exam.id(), 999_999L, owner.token()).expectStatus(404);
        invite(exam.id(), owner.id(), guest.token()).expectStatus(404);
        post("/api/shares/invitations/" + invitationId(guest.token()) + "/accept", owner.token(), null).expectStatus(404);
    }

    // ───── Gestionar el acceso ─────

    @Test
    @DisplayName("El dueño ve con quién lo comparte y puede retirar el acceso; el invitado puede dejar de verlo")
    void revokeAndLeave() {
        Session owner = newTeacher();
        Session first = newTeacher();
        Session second = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        share(exam.id(), owner, first);
        invite(exam.id(), second.id(), owner.token()).expectStatus(201);

        assertThat(shares(exam.id(), owner.token())).extracting(s -> s.get("username"), s -> s.get("status"))
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(first.username(), "ACCEPTED"),
                        org.assertj.core.groups.Tuple.tuple(second.username(), "PENDING"));

        delete("/api/exams/" + exam.id() + "/shares/" + first.id(), owner.token()).expectStatus(204);
        delete("/api/exams/" + exam.id() + "/shares/" + second.id(), owner.token()).expectStatus(204);
        get("/api/exams/" + exam.id(), first.token()).expectStatus(404);
        assertThat(invitations(second.token())).isEmpty();

        share(exam.id(), owner, first);
        delete("/api/exams/" + exam.id() + "/shares/me", first.token()).expectStatus(204);
        get("/api/exams/" + exam.id(), first.token()).expectStatus(404);
        assertThat(shares(exam.id(), owner.token())).isEmpty();
        delete("/api/exams/" + exam.id() + "/shares/me", first.token()).expectStatus(404);
    }

    @Test
    @DisplayName("Candidatos: profesores y administradores por nombre o usuario, sin el dueño ni los ya invitados")
    void candidates() {
        Session owner = newTeacher();
        Session invited = newTeacher();
        Session free = newTeacher();
        Session student = newStudent();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        invite(exam.id(), invited.id(), owner.token()).expectStatus(201);

        assertThat(candidates(exam.id(), free.username(), owner.token())).containsExactly(free.username());
        assertThat(candidates(exam.id(), invited.username(), owner.token())).isEmpty();
        assertThat(candidates(exam.id(), owner.username(), owner.token())).isEmpty();
        assertThat(candidates(exam.id(), student.username(), owner.token())).isEmpty();
    }

    @Test
    @DisplayName("Los resultados de un usuario (RF-55) incluyen los de los exámenes compartidos conmigo")
    void userResults_includeSharedExams() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        Session student = newStudent();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        submit(exam.code(), startAttempt(exam.code(), student.token(), "Yo"), student.token(),
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        share(exam.id(), owner, guest);

        assertThat((List<Integer>) get("/api/results/user/" + student.id(), guest.token()).expectStatus(200)
                .read("$.results[*].examId")).containsExactly((int) exam.id());
    }

    // ───── Ayudas ─────

    private HttpResult invite(long examId, long teacherId, String token) {
        return post("/api/exams/" + examId + "/shares", token, obj("teacherId", teacherId));
    }

    /** Invita y el invitado acepta. */
    private void share(long examId, Session owner, Session guest) {
        invite(examId, guest.id(), owner.token()).expectStatus(201);
        post("/api/shares/invitations/" + invitationId(guest.token()) + "/accept", guest.token(), null).expectStatus(200);
    }

    private List<String> candidates(long examId, String query, String token) {
        return get("/api/exams/" + examId + "/shares/candidates?query=" + query, token).expectStatus(200).read("$[*].username");
    }

    private List<Map<String, Object>> invitations(String token) {
        return get("/api/shares/invitations", token).expectStatus(200).asList();
    }

    private long invitationId(String token) {
        return ((Number) invitations(token).get(0).get("id")).longValue();
    }

    private List<Map<String, Object>> shares(long examId, String token) {
        return get("/api/exams/" + examId + "/shares", token).expectStatus(200).asList();
    }

    private List<Map<String, Object>> mine(String token) {
        return get("/api/exams/mine", token).expectStatus(200).asList();
    }
}
