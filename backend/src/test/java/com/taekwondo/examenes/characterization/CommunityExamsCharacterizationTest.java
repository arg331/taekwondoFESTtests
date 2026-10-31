package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-30, RF-31 y DE-12 (aprobados el 30/10/2026): el profesor ve los exámenes de prueba (públicos) de los
 * demás profesores, los guarda en favoritos y copia a su banco las preguntas que quiera.
 */
@DisplayName("Caracterización: exámenes de prueba de otros profesores")
class CommunityExamsCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Lista los públicos publicados de otros, con su autor y si son favoritos; ni borradores, ni privados, ni los míos")
    void list_foreignPublicExams() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam open = publishedExam(owner.token(), 5, null, true);
        PublishedExam closed = publishedExam(owner.token(), 5, null, true);
        post("/api/exams/" + closed.id() + "/close", owner.token(), null).expectStatus(200);
        PublishedExam turnedPrivate = publishedExam(owner.token(), 5, null, true);
        patch("/api/exams/" + turnedPrivate.id() + "/visibility", owner.token(), obj("newVisibility", "PRIVATE"));
        long draft = createDraft(owner.token(), 5, null, true);
        PublishedExam mine = publishedExam(me.token(), 5, null, true);
        post("/api/exams/" + open.id() + "/favorite", me.token(), null).expectStatus(204);

        List<Map<String, Object>> exams = get("/api/community/exams", me.token()).expectStatus(200).asList();

        assertThat(ids(exams)).contains(open.id(), closed.id())
                .doesNotContain(turnedPrivate.id(), draft, mine.id());
        Map<String, Object> first = byId(exams, open.id());
        assertThat(first).containsEntry("ownerName", "Alumno " + owner.username())
                .containsEntry("questionCount", 5)
                .containsEntry("status", "PUBLISHED")
                .containsEntry("favorite", true);
        assertThat(byId(exams, closed.id())).containsEntry("status", "EXPIRED").containsEntry("favorite", false);
    }

    @Test
    @DisplayName("Favoritos: salen con el mismo formato, ya marcados")
    void favorites_sameShape() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        post("/api/exams/" + exam.id() + "/favorite", me.token(), null).expectStatus(204);

        List<Map<String, Object>> favorites = get("/api/favorites", me.token()).expectStatus(200).asList();

        assertThat(favorites).singleElement().satisfies(f -> assertThat(f)
                .containsEntry("ownerName", "Alumno " + owner.username())
                .containsEntry("favorite", true)
                .containsEntry("questionCount", 5));
    }

    @Test
    @DisplayName("Preguntas: las publicadas, con solución, aunque el autor las edite después en su banco")
    void questions_asPublished() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        long first = exam.questionIds().get(0);
        String original = (String) get("/api/questions/" + first, owner.token()).read("$.text");
        put("/api/questions/" + first, owner.token(), obj("text", unique("Editada"), "options", FOUR_OPTIONS,
                "correctAnswers", List.of(3))).expectStatus(200);

        List<Map<String, Object>> questions = get("/api/community/exams/" + exam.id() + "/questions", me.token())
                .expectStatus(200).asList();

        assertThat(questions).hasSize(5);
        assertThat(questions.get(0)).containsEntry("questionId", (int) first)
                .containsEntry("text", original)
                .containsEntry("options", FOUR_OPTIONS)
                .containsEntry("correctAnswers", List.of(0))
                .containsEntry("explanation", "Explicación de " + original);
    }

    @Test
    @DisplayName("Preguntas: de un examen privado, borrador o inexistente 404; de uno mío 409")
    void questions_onlyForeignPublic() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam turnedPrivate = publishedExam(owner.token(), 5, null, true);
        patch("/api/exams/" + turnedPrivate.id() + "/visibility", owner.token(), obj("newVisibility", "PRIVATE"));
        long draft = createDraft(owner.token(), 5, null, true);
        PublishedExam mine = publishedExam(me.token(), 5, null, true);

        assertThat(get("/api/community/exams/" + turnedPrivate.id() + "/questions", me.token()).expectStatus(404).message())
                .isEqualTo("No existe un examen con id " + turnedPrivate.id());
        get("/api/community/exams/" + draft + "/questions", me.token()).expectStatus(404);
        get("/api/community/exams/999999/questions", me.token()).expectStatus(404);
        assertThat(get("/api/community/exams/" + mine.id() + "/questions", me.token()).expectStatus(409).message())
                .isEqualTo("Las preguntas de tus exámenes ya están en tu banco");
    }

    @Test
    @DisplayName("Copiar: las elegidas pasan a mi banco como mías, con grado; el examen y el banco del autor no cambian")
    void copy_selectedIntoMyBank() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        List<Long> chosen = exam.questionIds().subList(0, 2);
        int ownerBank = get("/api/questions", owner.token()).asList().size();

        HttpResult copy = post("/api/community/exams/" + exam.id() + "/copy-questions", me.token(),
                obj("questionIds", chosen));

        assertThat((Integer) copy.expectStatus(200).read("$.copied")).isEqualTo(2);
        assertThat((Integer) copy.read("$.alreadyInBank")).isZero();
        List<Map<String, Object>> myBank = get("/api/questions", me.token()).asList();
        assertThat(myBank).hasSize(2);
        assertThat(myBank).extracting(q -> ((Number) q.get("id")).longValue()).doesNotContainAnyElementsOf(chosen);
        Map<String, Object> copied = myBank.stream()
                .filter(q -> ((String) q.get("text")).startsWith("Pregunta 1 ")).findFirst().orElseThrow();
        assertThat(copied).containsEntry("options", FOUR_OPTIONS)
                .containsEntry("correctAnswers", List.of(1))
                .containsEntry("grade", "VERDE");
        assertThat((String) copied.get("explanation")).startsWith("Explicación de Pregunta 1 ");
        assertThat(get("/api/questions", owner.token()).asList()).hasSize(ownerBank);
    }

    @Test
    @DisplayName("Copiar otra vez: las que ya tengo en el banco no se duplican")
    void copy_twice_noDuplicates() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        post("/api/community/exams/" + exam.id() + "/copy-questions", me.token(),
                obj("questionIds", exam.questionIds().subList(0, 2))).expectStatus(200);

        HttpResult again = post("/api/community/exams/" + exam.id() + "/copy-questions", me.token(),
                obj("questionIds", exam.questionIds()));

        assertThat((Integer) again.expectStatus(200).read("$.copied")).isEqualTo(3);
        assertThat((Integer) again.read("$.alreadyInBank")).isEqualTo(2);
        assertThat(get("/api/questions", me.token()).asList()).hasSize(5);
    }

    @Test
    @DisplayName("Copiar: una pregunta que no es del examen 409; sin elegir ninguna 400")
    void copy_rules() {
        Session owner = newTeacher();
        Session me = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        long other = createQuestion(owner.token(), unique("Fuera"), FOUR_OPTIONS, 0, List.of());

        HttpResult foreign = post("/api/community/exams/" + exam.id() + "/copy-questions", me.token(),
                obj("questionIds", List.of(exam.questionIds().get(0), other)));
        HttpResult empty = post("/api/community/exams/" + exam.id() + "/copy-questions", me.token(),
                obj("questionIds", List.of()));

        assertThat(foreign.expectStatus(409).message()).isEqualTo("Esa pregunta no está en este examen");
        empty.expectStatus(400);
        assertThat(get("/api/questions", me.token()).asList()).isEmpty();
    }

    @Test
    @DisplayName("Solo profesores: un alumno recibe 403")
    void students_forbidden() {
        Session owner = newTeacher();
        Session student = newStudent();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);

        get("/api/community/exams", student.token()).expectStatus(403);
        get("/api/community/exams/" + exam.id() + "/questions", student.token()).expectStatus(403);
        post("/api/community/exams/" + exam.id() + "/copy-questions", student.token(),
                obj("questionIds", exam.questionIds())).expectStatus(403);
    }

    private static List<Long> ids(List<Map<String, Object>> items) {
        return items.stream().map(e -> ((Number) e.get("id")).longValue()).toList();
    }

    private static Map<String, Object> byId(List<Map<String, Object>> items, long id) {
        return items.stream().filter(e -> ((Number) e.get("id")).longValue() == id).findFirst().orElseThrow();
    }
}
