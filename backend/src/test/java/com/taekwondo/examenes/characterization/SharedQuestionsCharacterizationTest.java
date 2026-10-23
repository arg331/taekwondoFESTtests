package com.taekwondo.examenes.characterization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-57 (redefinido el 21/10/2026): las preguntas de los exámenes que me han compartido aparecen en
 * «Preguntas compartidas conmigo». Son las del banco del dueño, al día, y solo se consultan.
 */
@DisplayName("Caracterización: preguntas compartidas conmigo")
class SharedQuestionsCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Al aceptar, las preguntas del examen salen como compartidas, con quién y sus tags; no en mi banco")
    void accept_sharesExamQuestions() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        long tagId = createTag(owner.token(), unique("Reglamento"), "#c62828");
        List<Long> ids = new ArrayList<>(List.of(createQuestion(owner.token(), unique("Con tag"), FOUR_OPTIONS, 0, List.of(tagId))));
        ids.addAll(createQuestions(owner.token(), 4));
        long examId = draft(owner, ids);
        invite(examId, owner, guest);

        assertThat(shared(guest.token())).isEmpty();
        accept(guest);

        List<Map<String, Object>> shared = shared(guest.token());
        assertThat(shared).extracting(q -> ((Number) q.get("id")).longValue()).containsExactlyInAnyOrderElementsOf(ids);
        assertThat(shared).allSatisfy(q -> assertThat(q).containsEntry("sharedBy", "Alumno " + owner.username()));
        Map<String, Object> withTag = byId(shared, ids.get(0));
        assertThat((List<Map<String, Object>>) withTag.get("tags")).extracting(t -> ((Number) t.get("id")).longValue())
                .containsExactly(tagId);
        assertThat(get("/api/questions", guest.token()).expectStatus(200).asList())
                .extracting(q -> ((Number) q.get("id")).longValue()).doesNotContainAnyElementsOf(ids);
        assertThat((Object) get("/api/questions/" + ids.get(0), owner.token()).read("$.sharedBy")).isNull();
    }

    @Test
    @DisplayName("Son las del banco del dueño, al día; el invitado no puede cambiarlas ni borrarlas (404)")
    void sharedQuestions_liveAndReadOnly() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        List<Long> ids = createQuestions(owner.token(), 5);
        long examId = draft(owner, ids);
        invite(examId, owner, guest);
        accept(guest);
        long first = ids.get(0);
        String newText = unique("Texto nuevo");

        put("/api/questions/" + first, owner.token(), questionBody(newText)).expectStatus(200);

        assertThat(byId(shared(guest.token()), first)).containsEntry("text", newText);
        put("/api/questions/" + first, guest.token(), questionBody("Mío")).expectStatus(404);
        delete("/api/questions/" + first, guest.token()).expectStatus(404);
    }

    @Test
    @DisplayName("Una pregunta en dos exámenes compartidos sale una vez; al retirar el acceso dejan de salir")
    void sharedQuestions_onceAndRevoked() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        List<Long> ids = createQuestions(owner.token(), 5);
        long first = draft(owner, ids);
        long second = draft(owner, ids);
        invite(first, owner, guest);
        accept(guest);
        invite(second, owner, guest);
        accept(guest);

        assertThat(shared(guest.token())).hasSize(5);

        delete("/api/exams/" + first + "/shares/" + guest.id(), owner.token()).expectStatus(204);
        assertThat(shared(guest.token())).hasSize(5);
        delete("/api/exams/" + second + "/shares/" + guest.id(), owner.token()).expectStatus(204);
        assertThat(shared(guest.token())).isEmpty();
    }

    @Test
    @DisplayName("Las que el dueño elimina dejan de salir, aunque sigan en su examen publicado")
    void sharedQuestions_deletedByOwnerHidden() {
        Session owner = newTeacher();
        Session guest = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        invite(exam.id(), owner, guest);
        accept(guest);

        delete("/api/questions/" + exam.questionIds().get(0), owner.token()).expectStatus(204);

        assertThat(shared(guest.token())).extracting(q -> ((Number) q.get("id")).longValue())
                .hasSize(4).doesNotContain(exam.questionIds().get(0));
    }

    // ───── Ayudas ─────

    private long draft(Session owner, List<Long> questionIds) {
        long examId = createDraft(owner.token(), questionIds.size(), null, true);
        setQuestions(owner.token(), examId, questionIds);
        return examId;
    }

    private void invite(long examId, Session owner, Session guest) {
        post("/api/exams/" + examId + "/shares", owner.token(), obj("teacherId", guest.id())).expectStatus(201);
    }

    private void accept(Session guest) {
        long invitation = ((Number) get("/api/shares/invitations", guest.token()).expectStatus(200).asList().get(0)
                .get("id")).longValue();
        post("/api/shares/invitations/" + invitation + "/accept", guest.token(), null).expectStatus(200);
    }

    private List<Map<String, Object>> shared(String token) {
        return get("/api/shares/questions", token).expectStatus(200).asList();
    }

    private static Map<String, Object> byId(List<Map<String, Object>> questions, long id) {
        return questions.stream().filter(q -> ((Number) q.get("id")).longValue() == id).findFirst().orElseThrow();
    }

    private static Map<String, Object> questionBody(String text) {
        return obj("text", text, "options", FOUR_OPTIONS, "correctAnswers", List.of(0), "explanation", "",
                "grade", "VERDE", "tagIds", List.of());
    }
}
